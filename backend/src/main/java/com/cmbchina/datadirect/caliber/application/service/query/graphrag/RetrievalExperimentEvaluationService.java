package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag.GraphRuntimeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class RetrievalExperimentEvaluationService {

    private final GraphRuntimeProperties properties;

    public RetrievalExperimentEvaluationService(GraphRuntimeProperties properties) {
        this.properties = properties;
        this.properties.normalizeExperimentEvaluation();
    }

    public EvaluationReport evaluate(List<ExperimentReplaySample> samples) {
        properties.normalizeExperimentEvaluation();
        List<ExperimentReplaySample> safeSamples = samples == null ? List.of() : List.copyOf(samples);
        int sampleCount = safeSamples.size();
        int sceneHitCount = (int) safeSamples.stream().filter(ExperimentReplaySample::sceneHitAt5).count();
        int falseAllowCount = (int) safeSamples.stream().filter(ExperimentReplaySample::policyFalseAllow).count();
        int snapshotMismatchCount = (int) safeSamples.stream().filter(sample -> !sample.snapshotMatched()).count();
        long evidenceHitCount = safeSamples.stream().mapToLong(ExperimentReplaySample::correctEvidenceInTop10).sum();
        long evidenceWindow = safeSamples.stream().mapToLong(sample -> Math.max(10, sample.referenceCount())).sum();
        int observedFieldCount = safeSamples.stream().mapToInt(ExperimentReplaySample::observedFieldCount).sum();
        int requiredFieldCount = safeSamples.stream().mapToInt(sample ->
                Math.max(properties.getRequiredObservationFields(), sample.requiredFieldCount())).sum();
        int referenceCount = safeSamples.stream().mapToInt(ExperimentReplaySample::referenceCount).sum();
        int candidateCount = safeSamples.stream().mapToInt(ExperimentReplaySample::candidateCount).sum();

        double sceneHitAt5 = ratio(sceneHitCount, sampleCount);
        double evidencePrecisionAt10 = ratio(evidenceHitCount, evidenceWindow);
        double observedFieldCompleteness = ratio(observedFieldCount, requiredFieldCount);
        long p95FormalLatencyMs = percentile(safeSamples.stream()
                .map(ExperimentReplaySample::formalLatencyMs)
                .filter(value -> value != null && value >= 0)
                .toList());
        long p95ExperimentLatencyMs = percentile(safeSamples.stream()
                .map(ExperimentReplaySample::experimentLatencyMs)
                .filter(value -> value != null && value >= 0)
                .toList());

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (properties.isEmergencyStopEnabled()) {
            blockers.add("EMERGENCY_STOP_ENABLED");
        }
        if (falseAllowCount > 0) {
            blockers.add("POLICY_FALSE_ALLOW");
        }
        if (snapshotMismatchCount > 0) {
            blockers.add("SNAPSHOT_MISMATCH");
        }
        if (observedFieldCompleteness < 1.0d) {
            blockers.add("OBSERVATION_FIELDS_MISSING");
        }
        if (sceneHitAt5 < properties.getSceneHitAt5Threshold()) {
            blockers.add("SCENE_HIT_AT5_BELOW_THRESHOLD");
        }
        if (evidencePrecisionAt10 < properties.getEvidencePrecisionAt10Threshold()) {
            blockers.add("EVIDENCE_PRECISION_AT10_BELOW_THRESHOLD");
        }
        if (p95FormalLatencyMs > properties.getP95LatencyBudgetMs()) {
            blockers.add("FORMAL_P95_BUDGET_EXCEEDED");
        }

        String gateDecision;
        String rollbackRecommendation;
        if (properties.isEmergencyStopEnabled()) {
            gateDecision = "EMERGENCY_STOP";
            rollbackRecommendation = "STOP_GRAY_AND_SHADOW";
        } else if (falseAllowCount > 0 || snapshotMismatchCount > 0) {
            gateDecision = "ROLLBACK_REQUIRED";
            rollbackRecommendation = "DISABLE_EXPERIMENT_ADAPTER";
        } else if (!blockers.isEmpty() && properties.isShadowModeEnabled()) {
            gateDecision = "SHADOW_ONLY";
            rollbackRecommendation = "KEEP_SHADOW_ONLY";
        } else if (!blockers.isEmpty()) {
            gateDecision = "BLOCKED";
            rollbackRecommendation = "HOLD_OFFLINE";
        } else if (properties.isGrayReleaseEnabled()) {
            gateDecision = "GRAY_READY";
            rollbackRecommendation = "KEEP_SHADOW_AND_OPEN_GRAY";
        } else if (properties.isShadowModeEnabled()) {
            gateDecision = "SHADOW_ONLY_READY";
            rollbackRecommendation = "KEEP_SHADOW_ONLY";
        } else {
            gateDecision = "OFFLINE_READY";
            rollbackRecommendation = "READY_FOR_REVIEW";
        }

        List<String> warnings = new ArrayList<>();
        if (p95ExperimentLatencyMs > properties.getP95LatencyBudgetMs()) {
            warnings.add("EXPERIMENT_P95_BUDGET_EXCEEDED");
        }
        if (referenceCount == 0) {
            warnings.add("EMPTY_REFERENCES");
        }
        if (candidateCount == 0) {
            warnings.add("EMPTY_CANDIDATES");
        }

        EvaluationMetrics metrics = new EvaluationMetrics(
                sceneHitAt5,
                evidencePrecisionAt10,
                falseAllowCount,
                snapshotMismatchCount,
                observedFieldCompleteness,
                p95FormalLatencyMs,
                p95ExperimentLatencyMs,
                referenceCount,
                candidateCount,
                sampleCount
        );
        EvaluationSummary summary = new EvaluationSummary(
                gateDecision,
                rollbackRecommendation,
                properties.isShadowModeEnabled(),
                properties.isGrayReleaseEnabled(),
                properties.getGrayReleaseScope(),
                properties.getRetrievalAdapterName(),
                properties.getRetrievalIndexVersion(),
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
        return new EvaluationReport(summary, metrics, safeSamples);
    }

    public EvaluationReport evaluateDemoCorpus(String snapshotId, String adapterName) {
        if (snapshotId != null && !snapshotId.isBlank()) {
            properties.setRetrievalIndexVersion(snapshotId.trim());
        }
        if (adapterName != null && !adapterName.isBlank()) {
            properties.setRetrievalAdapterName(adapterName.trim());
        }
        return evaluate(List.of(
                new ExperimentReplaySample(
                        "trace-shadow-01",
                        101L,
                        properties.getRetrievalAdapterName(),
                        properties.getRetrievalIndexVersion(),
                        true,
                        8,
                        10,
                        false,
                        true,
                        1450L,
                        2100L,
                        properties.getRequiredObservationFields(),
                        properties.getRequiredObservationFields(),
                        6,
                        9
                ),
                new ExperimentReplaySample(
                        "trace-shadow-02",
                        101L,
                        properties.getRetrievalAdapterName(),
                        properties.getRetrievalIndexVersion(),
                        true,
                        7,
                        10,
                        false,
                        true,
                        1600L,
                        2300L,
                        properties.getRequiredObservationFields(),
                        properties.getRequiredObservationFields(),
                        5,
                        8
                ),
                new ExperimentReplaySample(
                        "trace-shadow-03",
                        101L,
                        properties.getRetrievalAdapterName(),
                        properties.getRetrievalIndexVersion(),
                        true,
                        6,
                        10,
                        false,
                        true,
                        1750L,
                        2500L,
                        properties.getRequiredObservationFields(),
                        properties.getRequiredObservationFields(),
                        7,
                        10
                )
        ));
    }

    public static void main(String[] args) throws Exception {
        GraphRuntimeProperties properties = new GraphRuntimeProperties();
        properties.setShadowModeEnabled(readBooleanEnv("CALIBER_GRAPH_SHADOW_MODE_ENABLED", true));
        properties.setGrayReleaseEnabled(readBooleanEnv("CALIBER_GRAPH_GRAY_RELEASE_ENABLED", true));
        properties.setEmergencyStopEnabled(readBooleanEnv("CALIBER_GRAPH_EMERGENCY_STOP_ENABLED", false));
        properties.setGrayReleaseScope(readStringEnv("CALIBER_GRAPH_GRAY_RELEASE_SCOPE", "domain:payroll"));
        properties.setSceneHitAt5Threshold(readDoubleEnv("CALIBER_GRAPH_SCENE_HIT_AT5_THRESHOLD", 0.85d));
        properties.setEvidencePrecisionAt10Threshold(readDoubleEnv("CALIBER_GRAPH_EVIDENCE_PRECISION_AT10_THRESHOLD", 0.70d));
        properties.setP95LatencyBudgetMs(readLongEnv("CALIBER_GRAPH_P95_LATENCY_BUDGET_MS", 8000L));
        properties.setRequiredObservationFields((int) readLongEnv("CALIBER_GRAPH_REQUIRED_OBSERVATION_FIELDS", 6L));

        String snapshotId = null;
        String adapterName = null;
        String grayScope = null;
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("--snapshot-id".equals(arg) && index + 1 < args.length) {
                snapshotId = args[++index];
            } else if ("--adapter".equals(arg) && index + 1 < args.length) {
                adapterName = args[++index];
            } else if ("--gray-scope".equals(arg) && index + 1 < args.length) {
                grayScope = args[++index];
            }
        }
        if (grayScope != null && !grayScope.isBlank()) {
            properties.setGrayReleaseScope(grayScope);
        }
        RetrievalExperimentEvaluationService service = new RetrievalExperimentEvaluationService(properties);
        EvaluationReport report = service.evaluateDemoCorpus(snapshotId, adapterName);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private static boolean readBooleanEnv(String key, boolean fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private static String readStringEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static double readDoubleEnv(String key, double fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long readLongEnv(String key, long fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0.0d;
        }
        return numerator / (double) denominator;
    }

    private long percentile(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = values.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        int rank = (int) Math.ceil(sorted.size() * 0.95d) - 1;
        rank = Math.max(0, Math.min(sorted.size() - 1, rank));
        return sorted.get(rank);
    }

    public record ExperimentReplaySample(
            String traceId,
            Long snapshotId,
            String adapterName,
            String indexVersion,
            boolean sceneHitAt5,
            int correctEvidenceInTop10,
            int evidenceWindow,
            boolean policyFalseAllow,
            boolean snapshotMatched,
            Long formalLatencyMs,
            Long experimentLatencyMs,
            int observedFieldCount,
            int requiredFieldCount,
            int referenceCount,
            int candidateCount
    ) {
        public ExperimentReplaySample {
            adapterName = normalizeText(adapterName, "LightRAG");
            indexVersion = normalizeText(indexVersion, "snapshot-published");
            correctEvidenceInTop10 = Math.max(0, Math.min(correctEvidenceInTop10, 10));
            evidenceWindow = Math.max(10, evidenceWindow);
            observedFieldCount = Math.max(0, observedFieldCount);
            requiredFieldCount = Math.max(1, requiredFieldCount);
            referenceCount = Math.max(0, referenceCount);
            candidateCount = Math.max(0, candidateCount);
        }
    }

    public record EvaluationMetrics(
            double sceneHitAt5,
            double evidencePrecisionAt10,
            int policyFalseAllowCount,
            int snapshotMismatchCount,
            double observedFieldCompleteness,
            long p95FormalLatencyMs,
            long p95ExperimentLatencyMs,
            int referenceCount,
            int candidateCount,
            int sampleCount
    ) {
    }

    public record EvaluationSummary(
            String gateDecision,
            String rollbackRecommendation,
            boolean shadowModeEnabled,
            boolean grayReleaseEnabled,
            String grayReleaseScope,
            String adapterName,
            String indexVersion,
            List<String> blockers,
            List<String> warnings
    ) {
    }

    public record EvaluationReport(
            EvaluationSummary summary,
            EvaluationMetrics metrics,
            List<ExperimentReplaySample> samples
    ) {
    }

    private static String normalizeText(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return text;
    }
}
