package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.NlFeedbackCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.NlQueryCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.NlFeedbackResultDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.NlQueryResultDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.ExecutionFeedbackMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.PlanIrAuditMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ExecutionFeedbackPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.PlanIrAuditPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NlPlanAppService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}_]+");

    private final SceneMapper sceneMapper;
    private final PlanIrAuditMapper planIrAuditMapper;
    private final ExecutionFeedbackMapper executionFeedbackMapper;
    private final ObjectMapper objectMapper;

    public NlPlanAppService(SceneMapper sceneMapper,
                            PlanIrAuditMapper planIrAuditMapper,
                            ExecutionFeedbackMapper executionFeedbackMapper,
                            ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.planIrAuditMapper = planIrAuditMapper;
        this.executionFeedbackMapper = executionFeedbackMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NlQueryResultDTO query(NlQueryCmd cmd) {
        String queryText = cmd.queryText().trim();
        List<ScenePO> publishedScenes = sceneMapper.findByCondition(null, null, SceneStatus.PUBLISHED, null);
        ScoredScene best = pickBestScene(queryText, publishedScenes);
        String decision;
        String riskLevel;
        List<String> evidence = new ArrayList<>();
        if (best.scene() == null) {
            decision = "deny";
            riskLevel = "HIGH";
            evidence.add("未检索到可用已发布场景");
        } else {
            evidence.add("命中场景：" + best.scene().getSceneTitle());
            evidence.add("综合评分：" + String.format(Locale.ROOT, "%.3f", best.score()));
            if (best.score() >= 0.2d) {
                decision = "allow";
                riskLevel = "LOW";
            } else {
                decision = "need_approval";
                riskLevel = "MEDIUM";
            }
        }

        Long sceneId = best.scene() == null ? null : best.scene().getId();
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("queryText", queryText);
        plan.put("sceneId", sceneId);
        plan.put("decision", decision);
        plan.put("riskLevel", riskLevel);
        plan.put("score", best.score());
        plan.put("evidence", evidence);

        PlanIrAuditPO audit = new PlanIrAuditPO();
        audit.setQueryText(queryText);
        audit.setSceneId(sceneId);
        audit.setDecision(decision);
        audit.setRiskLevel(riskLevel);
        audit.setPlanJson(writeJson(plan));
        audit.setCreatedBy(cmd.operator());
        audit.setCreatedAt(OffsetDateTime.now());
        PlanIrAuditPO saved = planIrAuditMapper.save(audit);

        String sceneCode = best.scene() == null ? null : best.scene().getSceneCode();
        String sceneTitle = best.scene() == null ? null : best.scene().getSceneTitle();
        return new NlQueryResultDTO(saved.getId(), decision, riskLevel, sceneId, sceneCode, sceneTitle, evidence);
    }

    @Transactional
    public NlFeedbackResultDTO feedback(NlFeedbackCmd cmd) {
        PlanIrAuditPO audit = planIrAuditMapper.findById(cmd.planAuditId())
                .orElseThrow(() -> new ResourceNotFoundException("plan audit not found: " + cmd.planAuditId()));
        ExecutionFeedbackPO feedback = new ExecutionFeedbackPO();
        feedback.setPlanAuditId(audit.getId());
        feedback.setSceneId(audit.getSceneId());
        feedback.setSuccess(Boolean.TRUE.equals(cmd.success()));
        feedback.setReason(cmd.reason());
        feedback.setSelectedPlan(cmd.selectedPlan());
        feedback.setCreatedAt(OffsetDateTime.now());
        executionFeedbackMapper.save(feedback);

        Double weight = 0d;
        if (audit.getSceneId() != null) {
            long total = executionFeedbackMapper.countBySceneId(audit.getSceneId());
            long success = executionFeedbackMapper.countBySceneIdAndSuccessTrue(audit.getSceneId());
            weight = total <= 0 ? 0d : (double) success / (double) total;
        }
        return new NlFeedbackResultDTO(audit.getId(), cmd.success(), weight);
    }

    private ScoredScene pickBestScene(String queryText, List<ScenePO> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return new ScoredScene(null, 0d);
        }
        String normalizedQuery = queryText.toLowerCase(Locale.ROOT);
        String[] tokens = TOKEN_SPLIT.split(normalizedQuery);
        ScoredScene best = new ScoredScene(null, 0d);
        for (ScenePO scene : scenes) {
            String corpus = (
                    safe(scene.getSceneTitle()) + " " +
                    safe(scene.getSceneDescription()) + " " +
                    safe(scene.getSqlVariantsJson())
            ).toLowerCase(Locale.ROOT);

            double base = 0d;
            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                if (corpus.contains(token)) {
                    base += 0.12d;
                }
            }
            if (corpus.contains(normalizedQuery)) {
                base += 0.2d;
            }
            long total = executionFeedbackMapper.countBySceneId(scene.getId());
            long success = executionFeedbackMapper.countBySceneIdAndSuccessTrue(scene.getId());
            double feedbackBoost = total <= 0 ? 0d : ((double) success / (double) total) * 0.3d;
            double score = base + feedbackBoost;
            if (score > best.score()) {
                best = new ScoredScene(scene, score);
            }
        }
        return best;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize plan ir failed", ex);
        }
    }

    private record ScoredScene(ScenePO scene, double score) {
    }
}

