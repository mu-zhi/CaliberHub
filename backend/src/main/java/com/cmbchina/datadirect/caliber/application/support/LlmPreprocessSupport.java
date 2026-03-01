package com.cmbchina.datadirect.caliber.application.support;

import java.util.List;

public interface LlmPreprocessSupport {

    default String preprocessToCaliberImportV2(String rawText, String sourceType) {
        return preprocessToCaliberImportV2(rawText, sourceType, null);
    }

    String preprocessToCaliberImportV2(String rawText, String sourceType, String sourceName);

    default String preprocessToCaliberImportV2ByLlm(String rawText, String sourceType) {
        return preprocessToCaliberImportV2ByLlm(rawText, sourceType, null);
    }

    String preprocessToCaliberImportV2ByLlm(String rawText, String sourceType, String sourceName);

    default PromptPreviewResult previewPrompt(String rawText, String sourceType) {
        return previewPrompt(rawText, sourceType, null, null, null);
    }

    PromptPreviewResult previewPrompt(String rawText,
                                      String sourceType,
                                      String preprocessSystemPrompt,
                                      String preprocessUserPromptTemplate,
                                      String prepSchemaJson);

    record PromptPreviewResult(
            String systemPrompt,
            String userPrompt,
            String promptFingerprint,
            String normalizedSourceType,
            int lineCount,
            List<String> warnings
    ) {
    }
}
