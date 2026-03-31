package com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caliber.graph-runtime")
public class GraphRuntimeProperties {

    private boolean projectionEnabled;
    private boolean readEnabled;
    private boolean vectorEnabled;
    private boolean rebuildEnabled = true;
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
}
