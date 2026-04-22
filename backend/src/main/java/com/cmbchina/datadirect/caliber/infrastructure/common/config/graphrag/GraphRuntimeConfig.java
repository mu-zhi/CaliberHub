package com.cmbchina.datadirect.caliber.infrastructure.common.config.graphrag;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphRuntimeProperties.class)
public class GraphRuntimeConfig {

    @Bean
    public Runnable graphRuntimeExperimentDefaults(GraphRuntimeProperties properties) {
        properties.normalizeExperimentEvaluation();
        return properties::normalizeExperimentEvaluation;
    }
}
