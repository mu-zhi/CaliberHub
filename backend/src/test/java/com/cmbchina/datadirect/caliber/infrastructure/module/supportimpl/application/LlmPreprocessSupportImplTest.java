package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application;

import com.sun.net.httpserver.HttpServer;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPrepSchemaJsonGenerator;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmPreprocessProperties;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.LlmSecretCodec;
import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.domain.support.LlmPreprocessConfigDomainSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LlmPreprocessSupportImplTest {

    private final LlmPreprocessProperties properties = new LlmPreprocessProperties();
    private final LlmSecretCodec llmSecretCodec = new LlmSecretCodec(properties);
    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final LlmPrepSchemaJsonGenerator llmPrepSchemaJsonGenerator = new LlmPrepSchemaJsonGenerator(objectMapper);
    private final LlmPreprocessConfigDomainSupport configDomainSupport = new LlmPreprocessConfigDomainSupport() {
        @Override
        public Optional<LlmPreprocessConfig> findSingleton() {
            return Optional.empty();
        }

        @Override
        public LlmPreprocessConfig save(LlmPreprocessConfig config) {
            return config;
        }
    };
    private final LlmPreprocessSupportImpl support =
            new LlmPreprocessSupportImpl(
                    objectMapper,
                    properties,
                    configDomainSupport,
                    llmSecretCodec,
                    llmPrepSchemaJsonGenerator
            );

    @Test
    void shouldGenerateCaliberImportV2Json() throws Exception {
        String rawText = """
                # 零售客户信息查询
                查询近 30 天客户基本信息
                ```sql
                SELECT c.cust_id, c.cust_name
                FROM dm_customer_info c
                WHERE c.dt >= '2026-01-01';
                ```
                """;

        String result = support.preprocessToCaliberImportV2(rawText, "PASTE_MD");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("doc_type").asText()).isEqualTo("CALIBER_IMPORT_V2");
        assertThat(root.path("source_type").asText()).isEqualTo("PASTE_MD");
        assertThat(root.path("scenes")).hasSize(1);
        assertThat(root.path("scenes").get(0).path("scene_title").asText()).contains("查询");
        assertThat(root.path("scenes").get(0).path("sql_variants")).hasSize(1);
        assertThat(root.path("scenes").get(0).path("sql_variants").get(0).path("sql_text").asText())
                .contains("FROM dm_customer_info");
        assertThat(root.path("scenes").get(0).path("sql_variants").get(0).path("source_tables").size()).isGreaterThan(0);
        assertThat(root.path("global").path("common_tables_hint").size()).isGreaterThan(0);
    }

    @Test
    void shouldPreferMethodCommentAsRuleSceneTitle() throws Exception {
        String rawText = """
                ```sql
                -- Step 1: 查询代发协议号
                -- 方法 1：根据公司户口号查询代发协议号
                SELECT PROTOCOL_NBR
                FROM NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T
                WHERE CARD_NBR IN ();
                ```
                """;

        String result = support.preprocessToCaliberImportV2(rawText, "FILE_SQL");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
        assertThat(root.path("scenes")).hasSize(1);
        assertThat(root.path("scenes").get(0).path("scene_title").asText())
                .isEqualTo("根据公司户口号查询代发协议号");
    }

    @Test
    void shouldSkipCodeMappingsWhenNoExplicitValuePairs() throws Exception {
        String rawText = """
                /*
                限制字段：
                CARD_NBR：代发协议号绑定的对公户口号
                PROTOCOL_NBR：代发协议号
                */
                ```sql
                SELECT CARD_TYPE, PROTOCOL_NBR
                FROM NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T
                WHERE CARD_NBR IN ();
                ```
                """;

        String result = support.preprocessToCaliberImportV2(rawText, "FILE_SQL");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("scenes")).hasSize(1);
        assertThat(root.path("scenes").get(0).path("code_mappings")).isEmpty();
    }

    @Test
    void shouldFallbackWhenSourceTypeUnknown() throws Exception {
        String result = support.preprocessToCaliberImportV2("纯文本无SQL", "UNKNOWN_TYPE");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("source_type").asText()).isEqualTo("PASTE_MD");
        assertThat(root.path("scenes").get(0).path("quality").path("warnings").size()).isGreaterThan(0);
        assertThat(root.path("parse_report").path("warnings").size()).isGreaterThan(0);
    }

    @Test
    void shouldNormalizeGenericSourceTypeAliases() throws Exception {
        String imageResult = support.preprocessToCaliberImportV2("OCR 输出内容", "IMAGE");
        JsonNode imageRoot = JsonMapper.builder().findAndAddModules().build().readTree(imageResult);
        assertThat(imageRoot.path("source_type").asText()).isEqualTo("IMAGE_OCR_TEXT");

        String fileResult = support.preprocessToCaliberImportV2("select 1;", "FILE");
        JsonNode fileRoot = JsonMapper.builder().findAndAddModules().build().readTree(fileResult);
        assertThat(fileRoot.path("source_type").asText()).isEqualTo("FILE_TXT");

        String textResult = support.preprocessToCaliberImportV2("普通文本", "TEXT");
        JsonNode textRoot = JsonMapper.builder().findAndAddModules().build().readTree(textResult);
        assertThat(textRoot.path("source_type").asText()).isEqualTo("PASTE_MD");
    }

    @Test
    void shouldFallbackToRuleWhenLlmDisabled() throws Exception {
        properties.setEnabled(false);

        String result = support.preprocessToCaliberImportV2ByLlm("SELECT 1;", "PASTE_MD");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("doc_type").asText()).isEqualTo("CALIBER_IMPORT_V2");
        assertThat(root.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
    }

    @Test
    void shouldParseDirtyJsonAndBindSqlSegmentsToScenes() throws Exception {
        AtomicReference<String> requestBodyRef = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String llmContent = """
                结果如下：
                ```json
                {
                  "prep_type": "CALIBER_PREP_V1",
                  "schema_version": "1.2.0",
                  "source_type": "PASTE_MD",
                  "doc_profile": {
                    "language": "zh",
                    "has_sql": true,
                    "estimated_scene_count": 2,
                    "ocr_noise_level": "LOW",
                    "is_truncated": false
                  },
                  "context": {
                    "domain_guess": "零售",
                    "document_title": "多场景测试"
                  },
                  "normalized_text": "多场景测试文档",
                  "scene_candidates": [
                    {
                      "scene_id": "S001",
                      "scene_title": "场景一",
                      "scene_description_hint": "查询客户基础信息",
                      "sql_segment_ids": ["SQL_001"],
                      "confidence": 0.93,
                      "evidence_lines": ["001", "002"]
                    },
                    {
                      "scene_id": "S002",
                      "scene_title": "场景二",
                      "scene_description_hint": "查询账户信息",
                      "sql_segment_ids": ["SQL_002"],
                      "confidence": 0.9,
                      "evidence_lines": [8]
                    }
                  ],
                  "sql_segments": [
                    {
                      "segment_id": "SQL_001",
                      "name_hint": "客户主表查询",
                      "applicable_period": "2014至今",
                      "sql_raw": "[004] SELECT c.cust_id, c.cust_name\\n[005] FROM dm_customer_info c\\n[006] WHERE c.dt >= '2026-01-01';",
                      "sql_type": "SELECT",
                      "is_complete": true,
                      "source_spans": [{"start_line": "4", "end_line": "6"}],
                      "warnings": []
                    },
                    {
                      "segment_id": "SQL_002",
                      "name_hint": "账户主表查询",
                      "applicable_period": "2018至今",
                      "sql_raw": "[010] SELECT a.acct_no\\n[011] FROM dm_account a\\n[012] WHERE a.stat = 'A';",
                      "sql_type": "SELECT",
                      "is_complete": true,
                      "source_spans": [{"start_line": "[010]", "end_line": "[012]"}],
                      "warnings": []
                    }
                  ],
                  "table_hints": [
                    {"table": "dm_customer_info", "from_segment_id": "SQL_001", "confidence": 0.92},
                    {"table": "dm_account", "from_segment_id": "SQL_002", "confidence": 0.92}
                  ],
                  "field_hints": [
                    {
                      "field": "CARD_GRD_CD",
                      "table": "dm_customer_info",
                      "meaning_hint": "卡等级码",
                      "extracted_mappings": {"020": "金卡", "040": "金葵花卡"},
                      "confidence": 0.9
                    }
                  ],
                  "risk_notes": [
                    {"title": "口径提醒", "text": "需核对历史口径差异", "risk": "MEDIUM", "evidence_lines": [2]}
                  ],
                  "carry_over_text": "补充文本",
                  "unresolved": [],
                  "quality": {
                    "confidence": 0.9,
                    "warnings": [],
                    "errors": []
                  }
                }
                ```
                """;
        String llmResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": %s
                      }
                    }
                  ]
                }
                """.formatted(JsonMapper.builder().findAndAddModules().build().writeValueAsString(llmContent));
        server.createContext("/v1/chat/completions", exchange -> {
            requestBodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = llmResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            properties.setEnabled(true);
            properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            properties.setModel("qwen3-max");
            properties.setFallbackToRule(false);
            String rawText = """
                    ## 场景一
                    查询客户基础信息
                    SELECT c.cust_id, c.cust_name
                    FROM dm_customer_info c
                    WHERE c.dt >= '2026-01-01';

                    ## 场景二
                    查询账户信息
                    SELECT a.acct_no
                    FROM dm_account a
                    WHERE a.stat = 'A';
                    """;

            String result = support.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "multi-scene.sql");
            JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

            assertThat(root.path("_meta").path("mode").asText()).isEqualTo("llm_enhanced");
            assertThat(root.path("scenes")).hasSize(2);
            assertThat(root.path("scenes").get(0).path("sql_variants")).hasSize(1);
            assertThat(root.path("scenes").get(1).path("sql_variants")).hasSize(1);
            assertThat(root.path("scenes").get(0).path("sql_variants").get(0).path("sql_text").asText()).contains("dm_customer_info");
            assertThat(root.path("scenes").get(1).path("sql_variants").get(0).path("sql_text").asText()).contains("dm_account");
            assertThat(root.path("scenes").get(0).path("source_evidence_lines").get(0).asInt()).isEqualTo(1);
            assertThat(root.path("scenes").get(0).path("code_mappings").get(0).path("mappings").path("020").asText()).isEqualTo("金卡");

            assertThat(requestBodyRef.get()).contains("[001]");
            assertThat(requestBodyRef.get()).contains("[001] ## 场景一");
            assertThat(requestBodyRef.get()).contains("\"enable_thinking\":false");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRetryThreeTimesAndSucceedBeforeFallback() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String llmContent = """
                {
                  "prep_type": "CALIBER_PREP_V1",
                  "schema_version": "1.2.0",
                  "source_type": "PASTE_MD",
                  "doc_profile": {
                    "language": "zh",
                    "has_sql": true,
                    "estimated_scene_count": 1,
                    "ocr_noise_level": "LOW",
                    "is_truncated": false
                  },
                  "context": {
                    "domain_guess": "零售",
                    "document_title": "重试成功"
                  },
                  "normalized_text": "重试成功",
                  "scene_candidates": [
                    {
                      "scene_id": "S001",
                      "scene_title": "场景重试",
                      "scene_description_hint": "重试后成功",
                      "sql_segment_ids": ["SQL_001"],
                      "confidence": 0.9,
                      "evidence_lines": [1]
                    }
                  ],
                  "sql_segments": [
                    {
                      "segment_id": "SQL_001",
                      "name_hint": "主查询",
                      "applicable_period": "2014-至今",
                      "sql_raw": "SELECT c.cust_id\\nFROM dm_customer_info c\\nWHERE c.dt >= '2026-01-01';",
                      "sql_type": "SELECT",
                      "is_complete": true,
                      "source_spans": [{"start_line": 2, "end_line": 4}],
                      "warnings": []
                    }
                  ],
                  "table_hints": [
                    {"table": "dm_customer_info", "from_segment_id": "SQL_001", "confidence": 0.95}
                  ],
                  "field_hints": [],
                  "risk_notes": [],
                  "carry_over_text": "",
                  "unresolved": [],
                  "quality": {
                    "confidence": 0.9,
                    "warnings": [],
                    "errors": []
                  }
                }
                """;
        String llmResponse = """
                {"choices":[{"message":{"content":%s}}]}
                """.formatted(JsonMapper.builder().findAndAddModules().build().writeValueAsString(llmContent));
        server.createContext("/v1/chat/completions", exchange -> {
            int current = requestCount.incrementAndGet();
            if (current <= 2) {
                byte[] body = "{\"error\":\"temporary\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(502, body.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
                return;
            }
            byte[] body = llmResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            properties.setEnabled(true);
            properties.setFallbackToRule(true);
            properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            properties.setModel("qwen3-max");
            String rawText = """
                    ## 场景
                    SELECT c.cust_id
                    FROM dm_customer_info c
                    WHERE c.dt >= '2026-01-01';
                    """;
            String result = support.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "retry-success.sql");
            JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

            assertThat(root.path("_meta").path("mode").asText()).isEqualTo("llm_enhanced");
            assertThat(requestCount.get()).isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldUseResponsesApiPayloadForOpenAiEndpoint() throws Exception {
        AtomicReference<String> requestBodyRef = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String llmResponse = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"prep_type\\":\\"CALIBER_PREP_V1\\",\\"schema_version\\":\\"1.2.0\\",\\"source_type\\":\\"PASTE_MD\\",\\"doc_profile\\":{\\"language\\":\\"zh\\",\\"has_sql\\":true,\\"estimated_scene_count\\":1,\\"ocr_noise_level\\":\\"LOW\\",\\"is_truncated\\":false},\\"context\\":{\\"domain_guess\\":\\"零售\\",\\"document_title\\":\\"Responses API 测试\\"},\\"normalized_text\\":\\"Responses API 测试\\",\\"scene_candidates\\":[{\\"scene_id\\":\\"S001\\",\\"scene_title\\":\\"场景一\\",\\"scene_description_hint\\":\\"查询客户信息\\",\\"sql_segment_ids\\":[\\"SQL_001\\"],\\"confidence\\":0.92,\\"evidence_lines\\":[1]}],\\"sql_segments\\":[{\\"segment_id\\":\\"SQL_001\\",\\"name_hint\\":\\"客户查询\\",\\"applicable_period\\":\\"2014至今\\",\\"sql_raw\\":\\"SELECT c.cust_id FROM dm_customer_info c;\\",\\"sql_type\\":\\"SELECT\\",\\"is_complete\\":true,\\"source_spans\\":[{\\"start_line\\":1,\\"end_line\\":1}],\\"warnings\\":[]}],\\"table_hints\\":[{\\"table\\":\\"dm_customer_info\\",\\"from_segment_id\\":\\"SQL_001\\",\\"confidence\\":0.9}],\\"field_hints\\":[],\\"risk_notes\\":[],\\"carry_over_text\\":\\"\\",\\"unresolved\\":[],\\"quality\\":{\\"confidence\\":0.9,\\"warnings\\":[],\\"errors\\":[]}}"
                        }
                      ]
                    }
                  ]
                }
                """;
        server.createContext("/v1/responses", exchange -> {
            requestBodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = llmResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            properties.setEnabled(true);
            properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
            properties.setModel("gpt-5.4");
            properties.setFallbackToRule(false);
            properties.setEnableThinking(false);
            String result = support.preprocessToCaliberImportV2ByLlm("SELECT c.cust_id FROM dm_customer_info c;", "PASTE_MD", "responses.sql");
            JsonNode root = objectMapper.readTree(result);

            assertThat(root.path("_meta").path("mode").asText()).isEqualTo("llm_enhanced");
            assertThat(root.path("scenes")).hasSize(1);
            assertThat(root.path("scenes").get(0).path("sql_variants").get(0).path("sql_text").asText())
                    .contains("dm_customer_info");

            String requestBody = requestBodyRef.get();
            assertThat(requestBody).contains("\"instructions\"");
            assertThat(requestBody).contains("\"input\"");
            assertThat(requestBody).contains("\"text\":{\"format\":{\"type\":\"json_object\"}}");
            assertThat(requestBody).doesNotContain("\"messages\"");
            assertThat(requestBody).doesNotContain("\"response_format\"");
            assertThat(requestBody).doesNotContain("\"enable_thinking\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldFallbackToRuleWhenLlmRateLimitExceeded() throws Exception {
        LlmPreprocessProperties localProperties = new LlmPreprocessProperties();
        localProperties.setSecretKey("test-llm-secret-for-rate-limit");
        localProperties.setEnabled(true);
        localProperties.setFallbackToRule(true);
        localProperties.setRateLimitPerMinute(1);
        localProperties.setCircuitBreakerFailureThreshold(5);
        localProperties.setCircuitBreakerOpenSeconds(30);
        LlmSecretCodec localCodec = new LlmSecretCodec(localProperties);
        LlmPrepSchemaJsonGenerator localSchemaGenerator = new LlmPrepSchemaJsonGenerator(objectMapper);
        LlmPreprocessSupportImpl localSupport = new LlmPreprocessSupportImpl(
                objectMapper,
                localProperties,
                configDomainSupport,
                localCodec,
                localSchemaGenerator
        );
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String llmResponse = """
                {"choices":[{"message":{"content":"{\\"prep_type\\":\\"CALIBER_PREP_V1\\",\\"schema_version\\":\\"1.2.0\\",\\"source_type\\":\\"PASTE_MD\\",\\"doc_profile\\":{\\"language\\":\\"zh\\",\\"has_sql\\":true,\\"estimated_scene_count\\":1,\\"ocr_noise_level\\":\\"LOW\\",\\"is_truncated\\":false},\\"context\\":{\\"domain_guess\\":\\"零售\\",\\"document_title\\":\\"限流测试\\"},\\"normalized_text\\":\\"限流测试\\",\\"scene_candidates\\":[{\\"scene_id\\":\\"S001\\",\\"scene_title\\":\\"限流场景\\",\\"scene_description_hint\\":\\"测试\\",\\"sql_segment_ids\\":[\\"SQL_001\\"],\\"confidence\\":0.9,\\"evidence_lines\\":[1]}],\\"sql_segments\\":[{\\"segment_id\\":\\"SQL_001\\",\\"name_hint\\":\\"主查询\\",\\"applicable_period\\":\\"2014-至今\\",\\"sql_raw\\":\\"SELECT 1;\\",\\"sql_type\\":\\"SELECT\\",\\"is_complete\\":true,\\"source_spans\\":[{\\"start_line\\":1,\\"end_line\\":1}],\\"warnings\\":[]}],\\"table_hints\\":[],\\"field_hints\\":[],\\"risk_notes\\":[],\\"carry_over_text\\":\\"\\",\\"unresolved\\":[],\\"quality\\":{\\"confidence\\":0.9,\\"warnings\\":[],\\"errors\\":[]}}"}}]}
                """;
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = llmResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            localProperties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            localProperties.setModel("qwen3-max");
            String rawText = "SELECT 1;";

            String first = localSupport.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "rate-limit-1.sql");
            JsonNode firstRoot = objectMapper.readTree(first);
            assertThat(firstRoot.path("_meta").path("mode").asText()).isEqualTo("llm_enhanced");

            String second = localSupport.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "rate-limit-2.sql");
            JsonNode secondRoot = objectMapper.readTree(second);
            assertThat(secondRoot.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
            assertThat(secondRoot.path("parse_report").path("warnings").toString())
                    .contains("llm_rate_limited_fallback_to_rule")
                    .contains("llm_preprocess_fallback_to_rule");
            assertThat(requestCount.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldFallbackAfterThreeRetriesAndUseLowConfidence() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = "{\"error\":\"always-fail\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(502, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            properties.setEnabled(true);
            properties.setFallbackToRule(true);
            properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            properties.setModel("qwen3-max");
            String rawText = """
                    ## 回退测试
                    SELECT 1;
                    """;
            String result = support.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "retry-fallback.sql");
            JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

            assertThat(root.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
            assertThat(root.path("parse_report").path("warnings").toString()).contains("llm_preprocess_fallback_to_rule");
            assertThat(root.path("scenes").get(0).path("quality").path("confidence").asDouble()).isLessThan(0.70);
            assertThat(requestCount.get()).isEqualTo(4);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldOpenCircuitAfterConsecutiveFailuresAndFastFallback() throws Exception {
        LlmPreprocessProperties localProperties = new LlmPreprocessProperties();
        localProperties.setSecretKey("test-llm-secret-for-circuit");
        localProperties.setEnabled(true);
        localProperties.setFallbackToRule(true);
        localProperties.setRateLimitPerMinute(50);
        localProperties.setCircuitBreakerFailureThreshold(2);
        localProperties.setCircuitBreakerOpenSeconds(30);
        LlmSecretCodec localCodec = new LlmSecretCodec(localProperties);
        LlmPrepSchemaJsonGenerator localSchemaGenerator = new LlmPrepSchemaJsonGenerator(objectMapper);
        LlmPreprocessSupportImpl localSupport = new LlmPreprocessSupportImpl(
                objectMapper,
                localProperties,
                configDomainSupport,
                localCodec,
                localSchemaGenerator
        );
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = "{\"error\":\"always-fail\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(502, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            localProperties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            localProperties.setModel("qwen3-max");
            String rawText = "SELECT 1;";

            String first = localSupport.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "circuit-1.sql");
            JsonNode firstRoot = objectMapper.readTree(first);
            assertThat(firstRoot.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
            assertThat(firstRoot.path("parse_report").path("warnings").toString())
                    .contains("llm_circuit_opened_fallback_to_rule")
                    .contains("llm_preprocess_fallback_to_rule");
            assertThat(requestCount.get()).isEqualTo(2);

            String second = localSupport.preprocessToCaliberImportV2ByLlm(rawText, "PASTE_MD", "circuit-2.sql");
            JsonNode secondRoot = objectMapper.readTree(second);
            assertThat(secondRoot.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
            assertThat(secondRoot.path("parse_report").path("warnings").toString())
                    .contains("llm_circuit_open_fallback_to_rule")
                    .contains("llm_preprocess_fallback_to_rule");
            assertThat(requestCount.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldSplitSingleFencedCodeBlockIntoMultipleRuleVariants() throws Exception {
        String rawText = """
                # 代发明细查询
                ```sql
                -- Step 1: 查询协议号
                SELECT PROTOCOL_NBR
                FROM NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T
                WHERE CARD_NBR = 'A001';

                -- Step 2: 查询历史明细
                SELECT *
                FROM LGC_EAM.UNICORE_EPHISTRXP_YEAR
                WHERE TRX_DAT BETWEEN '2011-01-01' AND '2012-12-31'

                -- Step 3: 查询当前明细
                SELECT *
                FROM PDM_VHIS.T05_AGN_DTL
                WHERE TRX_DT BETWEEN '2023-01-01' AND '2023-12-31';
                ```
                """;

        String result = support.preprocessToCaliberImportV2(rawText, "FILE_SQL");
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("scenes").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldGenerateAtLeastSixScenesForDoc11WhenFallback() throws Exception {
        Path doc11Path = Path.of(
                "..", "research", "source-materials", "sql-samples", "05-口径文档现状-代发明细查询.sql");
        assumeTrue(Files.exists(doc11Path), "doc11 fixture is required for this test");
        String rawText = Files.readString(doc11Path, StandardCharsets.UTF_8);

        properties.setEnabled(true);
        properties.setFallbackToRule(true);
        properties.setEndpoint("http://127.0.0.1:1/v1/chat/completions");
        properties.setModel("qwen3-max");
        String result = support.preprocessToCaliberImportV2ByLlm(rawText, "FILE_SQL", doc11Path.getFileName().toString());
        JsonNode root = JsonMapper.builder().findAndAddModules().build().readTree(result);

        assertThat(root.path("_meta").path("mode").asText()).isEqualTo("rule_generated");
        assertThat(root.path("scenes").size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void shouldRejectWhenLlmInputLinesExceedLimit() {
        properties.setEnabled(true);
        properties.setFallbackToRule(false);
        String oversized = ("x\n").repeat(10_001);

        assertThatThrownBy(() -> support.preprocessToCaliberImportV2ByLlm(oversized, "PASTE_MD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("文本过长，请分段导入");
    }
}
