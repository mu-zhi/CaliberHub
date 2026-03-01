package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LlmPrepSchemaJsonGenerator {

    private final ObjectMapper objectMapper;

    public LlmPrepSchemaJsonGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateSchemaJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(LlmPrepSchemaTemplate.sample());
        } catch (JsonProcessingException ex) {
            return LlmPromptDefaults.PREP_SCHEMA_JSON;
        }
    }
}
