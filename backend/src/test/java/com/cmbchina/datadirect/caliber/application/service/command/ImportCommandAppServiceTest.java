package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.PreprocessImportCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.PreprocessResultDTO;
import com.cmbchina.datadirect.caliber.application.support.LlmPreprocessSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportCommandAppServiceTest {

    private static final Pattern CHUNK_INDEX_PATTERN = Pattern.compile("\\[chunk\\s+(\\d+)/(\\d+)]");

    @Mock
    private LlmPreprocessSupport llmPreprocessSupport;

    @AfterEach
    void clearInterruptedState() {
        Thread.interrupted();
    }

    @Test
    void shouldUseLlmFirstForDefaultPreprocess() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd("raw-content", "PASTE_MD", "零售客户信息查询.sql", null, false, "tester");
        String responseJson = "{\"doc_type\":\"CALIBER_IMPORT_V2\",\"_meta\":{\"mode\":\"llm_enhanced\"},\"scenes\":[]}";
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(eq("raw-content"), eq("PASTE_MD"), eq("零售客户信息查询.sql")))
                .thenReturn(responseJson);

        PreprocessResultDTO result = importCommandAppService.preprocess(cmd);

        assertThat(result.caliberImportJson()).contains("CALIBER_IMPORT_V2");
        assertThat(result.mode()).isEqualTo("llm_enhanced");
        verify(llmPreprocessSupport).preprocessToCaliberImportV2ByLlm("raw-content", "PASTE_MD", "零售客户信息查询.sql");
    }

    @Test
    void shouldDelegateToLlmEnhancedSupport() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd("raw-content", "PASTE_MD", null, null, false, "tester");
        String responseJson = "{\"doc_type\":\"CALIBER_IMPORT_V2\",\"_meta\":{\"mode\":\"llm_enhanced\"},\"scenes\":[]}";
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(eq("raw-content"), eq("PASTE_MD"), eq(null)))
                .thenReturn(responseJson);

        PreprocessResultDTO result = importCommandAppService.preprocessByLlm(cmd);

        assertThat(result.mode()).isEqualTo("llm_enhanced");
        verify(llmPreprocessSupport).preprocessToCaliberImportV2ByLlm("raw-content", "PASTE_MD", null);
    }

    @Test
    void shouldExposeConfidenceMeta() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd("raw-content", "PASTE_MD", null, null, false, "tester");
        String responseJson = """
                {
                  "_meta": {"mode":"llm_enhanced"},
                  "scenes": [],
                  "quality": {"confidence": 0.66, "warnings": [], "errors": []},
                  "parse_report": {"warnings": []}
                }
                """;
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(eq("raw-content"), eq("PASTE_MD"), eq(null)))
                .thenReturn(responseJson);

        PreprocessResultDTO result = importCommandAppService.preprocess(cmd);

        assertThat(result.confidenceScore()).isEqualTo(0.66);
        assertThat(result.confidenceLevel()).isEqualTo("LOW");
        assertThat(result.lowConfidence()).isTrue();
        assertThat(result.warnings()).contains("confidence_below_threshold");
    }

    @Test
    void shouldRejectWhenRawTextExceedsLineLimit() {
        ImportCommandAppService importCommandAppService = newService();
        String rawText = ("line\n").repeat(10_001);
        PreprocessImportCmd cmd = new PreprocessImportCmd(rawText, "PASTE_MD", "oversized.sql", null, false, "tester");

        assertThatThrownBy(() -> importCommandAppService.preprocess(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("文本过长，请分段导入");

        verify(llmPreprocessSupport, never()).preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "oversized.sql");
    }

    @Test
    void shouldExecuteChunkedPreprocessConcurrently() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd(buildLargeRawText(), "FILE_SQL", "bulk-doc.sql", null, false, "tester");
        AtomicInteger callCount = new AtomicInteger();
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(org.mockito.ArgumentMatchers.anyString(), eq("FILE_SQL"), org.mockito.ArgumentMatchers.contains("[chunk ")))
                .thenAnswer(invocation -> {
                    callCount.incrementAndGet();
                    sleepSilently(220);
                    int chunkIndex = parseChunkIndex(invocation.getArgument(2, String.class));
                    return buildSceneJson("chunk-" + chunkIndex);
                });

        long startedAt = System.nanoTime();
        PreprocessResultDTO result = importCommandAppService.preprocess(cmd);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        int actualCalls = callCount.get();
        assertThat(actualCalls).isGreaterThanOrEqualTo(2);
        long serialUpperBoundMs = actualCalls * 220L;
        assertThat(elapsedMs).isLessThan(serialUpperBoundMs - 120L);
        assertThat(result.mode()).isEqualTo("llm_enhanced");
    }

    @Test
    void shouldKeepChunkMergeOrderStable() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd(buildLargeRawText(), "FILE_SQL", "ordered-doc.sql", null, false, "tester");
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(org.mockito.ArgumentMatchers.anyString(), eq("FILE_SQL"), org.mockito.ArgumentMatchers.contains("[chunk ")))
                .thenAnswer(invocation -> {
                    int chunkIndex = parseChunkIndex(invocation.getArgument(2, String.class));
                    long delayMs = (5 - chunkIndex) * 50L;
                    sleepSilently(Math.max(delayMs, 20));
                    return buildSceneJson("chunk-" + chunkIndex);
                });

        PreprocessResultDTO result = importCommandAppService.preprocess(cmd);

        List<String> titles = result.scenes().stream()
                .map(scene -> scene.path("scene_title").asText(""))
                .toList();
        assertThat(titles).isNotEmpty();
        for (int i = 0; i < titles.size(); i++) {
            assertThat(titles.get(i)).isEqualTo("chunk-" + (i + 1));
        }
    }

    @Test
    void shouldPropagateChunkExecutionFailure() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd(buildLargeRawText(), "FILE_SQL", "failure-doc.sql", null, false, "tester");
        when(llmPreprocessSupport.preprocessToCaliberImportV2ByLlm(org.mockito.ArgumentMatchers.anyString(), eq("FILE_SQL"), org.mockito.ArgumentMatchers.contains("[chunk ")))
                .thenAnswer(invocation -> {
                    int chunkIndex = parseChunkIndex(invocation.getArgument(2, String.class));
                    if (chunkIndex == 2) {
                        throw new IllegalStateException("simulated chunk failure");
                    }
                    return buildSceneJson("chunk-" + chunkIndex);
                });

        assertThatThrownBy(() -> importCommandAppService.preprocess(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("分块预处理失败")
                .hasRootCauseMessage("simulated chunk failure");
    }

    @Test
    void shouldRespectInterruptDuringChunkedProcessing() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd(buildLargeRawText(), "FILE_SQL", "interrupt-doc.sql", null, false, "tester");

        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> importCommandAppService.preprocess(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("分块预处理被中断");
    }

    @Test
    void shouldUseRulePipelineWhenPreprocessModeIsRuleOnly() {
        ImportCommandAppService importCommandAppService = newService();
        PreprocessImportCmd cmd = new PreprocessImportCmd(
                "### 场景标题：规则直导\n-- Step 1\nSELECT 1;",
                "PASTE_MD",
                "rule.md",
                "RULE_ONLY",
                false,
                "tester"
        );
        String responseJson = "{\"doc_type\":\"CALIBER_IMPORT_V2\",\"_meta\":{\"mode\":\"rule_generated\"},\"scenes\":[]}";
        when(llmPreprocessSupport.preprocessToCaliberImportV2(eq(cmd.rawText()), eq("PASTE_MD"), eq("rule.md")))
                .thenReturn(responseJson);

        PreprocessResultDTO result = importCommandAppService.preprocess(cmd);

        assertThat(result.mode()).isEqualTo("rule_generated");
        verify(llmPreprocessSupport).preprocessToCaliberImportV2(cmd.rawText(), "PASTE_MD", "rule.md");
        verify(llmPreprocessSupport, never()).preprocessToCaliberImportV2ByLlm(cmd.rawText(), "PASTE_MD", "rule.md");
    }

    private String buildLargeRawText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 320; i++) {
            builder.append("SELECT CUST_ID_").append(i)
                    .append(", BRANCH_ID_").append(i)
                    .append(" FROM T_CLIENT_").append(i)
                    .append(" WHERE AGN_BCH_SEQ = '")
                    .append(i)
                    .append("' AND STAT_CD = 'A';\n");
        }
        return builder.toString();
    }

    private int parseChunkIndex(String sourceName) {
        Matcher matcher = CHUNK_INDEX_PATTERN.matcher(sourceName == null ? "" : sourceName);
        if (!matcher.find()) {
            return 1;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String buildSceneJson(String title) {
        return """
                {
                  "doc_type": "CALIBER_IMPORT_V2",
                  "schema_version": "2.0.0",
                  "source_type": "FILE_SQL",
                  "global": {},
                  "scenes": [
                    {
                      "scene_title": "%s",
                      "quality": {"confidence": 0.9, "warnings": [], "errors": []}
                    }
                  ],
                  "quality": {"confidence": 0.9, "warnings": [], "errors": []},
                  "parse_report": {"warnings": [], "errors": []},
                  "_meta": {"mode": "llm_enhanced"}
                }
                """.formatted(title);
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("sleep interrupted", ex);
        }
    }

    private ImportCommandAppService newService() {
        return new ImportCommandAppService(
                llmPreprocessSupport,
                new ObjectMapper(),
                new SimpleMeterRegistry()
        );
    }
}
