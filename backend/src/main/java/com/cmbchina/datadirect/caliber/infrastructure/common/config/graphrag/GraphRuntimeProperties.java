package com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caliber.graph-runtime")
public class GraphRuntimeProperties {

    private boolean projectionEnabled;
    private boolean readEnabled;
    private boolean vectorEnabled;
    private boolean rebuildEnabled = true;
    private boolean shadowModeEnabled = true;
    private boolean grayReleaseEnabled;
    private boolean emergencyStopEnabled;
    private String grayReleaseScope = "domain:payroll";
    private String retrievalAdapterName = "LightRAG";
    private String retrievalIndexVersion = "snapshot-published";
    private double sceneHitAt5Threshold = 0.85d;
    private double evidencePrecisionAt10Threshold = 0.70d;
    private long p95LatencyBudgetMs = 8000L;
    private int requiredObservationFields = 6;
    private String neo4jUri;
    private String neo4jUsername;
    private String neo4jPassword;
    private String neo4jDatabase = "neo4j";
    private String embeddingProvider = "HASH";
    private String embeddingModel = "hash-bow-v1";
    private int embeddingDimension = 32;

    public boolean isProjectionEnabled() {
        return projectionEnabled;
    }

    public void setProjectionEnabled(boolean projectionEnabled) {
        this.projectionEnabled = projectionEnabled;
    }

    public boolean isReadEnabled() {
        return readEnabled;
    }

    public void setReadEnabled(boolean readEnabled) {
        this.readEnabled = readEnabled;
    }

    public boolean isVectorEnabled() {
        return vectorEnabled;
    }

    public void setVectorEnabled(boolean vectorEnabled) {
        this.vectorEnabled = vectorEnabled;
    }

    public boolean isRebuildEnabled() {
        return rebuildEnabled;
    }

    public void setRebuildEnabled(boolean rebuildEnabled) {
        this.rebuildEnabled = rebuildEnabled;
    }

    public boolean isShadowModeEnabled() {
        return shadowModeEnabled;
    }

    public void setShadowModeEnabled(boolean shadowModeEnabled) {
        this.shadowModeEnabled = shadowModeEnabled;
    }

    public boolean isGrayReleaseEnabled() {
        return grayReleaseEnabled;
    }

    public void setGrayReleaseEnabled(boolean grayReleaseEnabled) {
        this.grayReleaseEnabled = grayReleaseEnabled;
    }

    public boolean isEmergencyStopEnabled() {
        return emergencyStopEnabled;
    }

    public void setEmergencyStopEnabled(boolean emergencyStopEnabled) {
        this.emergencyStopEnabled = emergencyStopEnabled;
    }

    public String getGrayReleaseScope() {
        return grayReleaseScope;
    }

    public void setGrayReleaseScope(String grayReleaseScope) {
        this.grayReleaseScope = grayReleaseScope;
    }

    public String getRetrievalAdapterName() {
        return retrievalAdapterName;
    }

    public void setRetrievalAdapterName(String retrievalAdapterName) {
        this.retrievalAdapterName = retrievalAdapterName;
    }

    public String getRetrievalIndexVersion() {
        return retrievalIndexVersion;
    }

    public void setRetrievalIndexVersion(String retrievalIndexVersion) {
        this.retrievalIndexVersion = retrievalIndexVersion;
    }

    public double getSceneHitAt5Threshold() {
        return sceneHitAt5Threshold;
    }

    public void setSceneHitAt5Threshold(double sceneHitAt5Threshold) {
        this.sceneHitAt5Threshold = sceneHitAt5Threshold;
    }

    public double getEvidencePrecisionAt10Threshold() {
        return evidencePrecisionAt10Threshold;
    }

    public void setEvidencePrecisionAt10Threshold(double evidencePrecisionAt10Threshold) {
        this.evidencePrecisionAt10Threshold = evidencePrecisionAt10Threshold;
    }

    public long getP95LatencyBudgetMs() {
        return p95LatencyBudgetMs;
    }

    public void setP95LatencyBudgetMs(long p95LatencyBudgetMs) {
        this.p95LatencyBudgetMs = p95LatencyBudgetMs;
    }

    public int getRequiredObservationFields() {
        return requiredObservationFields;
    }

    public void setRequiredObservationFields(int requiredObservationFields) {
        this.requiredObservationFields = requiredObservationFields;
    }

    public String getNeo4jUri() {
        return neo4jUri;
    }

    public void setNeo4jUri(String neo4jUri) {
        this.neo4jUri = neo4jUri;
    }

    public String getNeo4jUsername() {
        return neo4jUsername;
    }

    public void setNeo4jUsername(String neo4jUsername) {
        this.neo4jUsername = neo4jUsername;
    }

    public String getNeo4jPassword() {
        return neo4jPassword;
    }

    public void setNeo4jPassword(String neo4jPassword) {
        this.neo4jPassword = neo4jPassword;
    }

    public String getNeo4jDatabase() {
        return neo4jDatabase;
    }

    public void setNeo4jDatabase(String neo4jDatabase) {
        this.neo4jDatabase = neo4jDatabase;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public void normalizeExperimentEvaluation() {
        if (grayReleaseScope == null || grayReleaseScope.isBlank()) {
            grayReleaseScope = "domain:payroll";
        } else {
            grayReleaseScope = grayReleaseScope.trim();
        }
        if (retrievalAdapterName == null || retrievalAdapterName.isBlank()) {
            retrievalAdapterName = "LightRAG";
        } else {
            retrievalAdapterName = retrievalAdapterName.trim();
        }
        if (retrievalIndexVersion == null || retrievalIndexVersion.isBlank()) {
            retrievalIndexVersion = "snapshot-published";
        } else {
            retrievalIndexVersion = retrievalIndexVersion.trim();
        }
        sceneHitAt5Threshold = clamp(sceneHitAt5Threshold, 0.0d, 1.0d, 0.85d);
        evidencePrecisionAt10Threshold = clamp(evidencePrecisionAt10Threshold, 0.0d, 1.0d, 0.70d);
        if (p95LatencyBudgetMs <= 0L) {
            p95LatencyBudgetMs = 8000L;
        }
        if (requiredObservationFields <= 0) {
            requiredObservationFields = 6;
        }
    }

    private double clamp(double value, double min, double max, double fallback) {
        if (Double.isNaN(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
