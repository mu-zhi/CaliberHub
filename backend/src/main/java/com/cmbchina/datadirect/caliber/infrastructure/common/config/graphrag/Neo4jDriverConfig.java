package com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jDriverConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "caliber.graph-runtime", name = "neo4j-uri")
    public Driver neo4jDriver(GraphRuntimeProperties properties) {
        return GraphDatabase.driver(
                properties.getNeo4jUri(),
                AuthTokens.basic(properties.getNeo4jUsername(), properties.getNeo4jPassword()));
    }
}
