package com.caliberhub.adapter.importformat;

import com.caliberhub.application.importdoc.schema.ImportSchemaV1;
import com.caliberhub.application.importdoc.schema.ImportTemplateGenerator;
import com.caliberhub.application.importdoc.schema.ImportPromptSnippetGenerator;
import com.caliberhub.application.importdoc.schema.ImportSchemaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 导入格式导出控制器
 * 
 * 提供导入格式的 Schema 和 Template 导出能力。
 */
@Slf4j
@RestController
@RequestMapping("/api/import-format")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ImportFormatController {

        private final ImportSchemaValidator validator;

        /**
         * 获取导入 Schema
         * 
         * GET /api/import-format/schema
         * 
         * @param version Schema 版本，目前仅支持 v1
         * @param lang    语言，目前仅支持 zh
         * @return JSON Schema (Draft 2020-12)
         */
        @GetMapping("/schema")
        public ResponseEntity<Map<String, Object>> getSchema(
                        @RequestParam(defaultValue = "v1") String version,
                        @RequestParam(defaultValue = "zh") String lang) {

                log.info("获取导入 Schema: version={}, lang={}", version, lang);

                if (!"v1".equals(version)) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error_code", "UNSUPPORTED_VERSION",
                                        "message", "不支持的版本：" + version + "，当前仅支持 v1"));
                }

                Map<String, Object> schema = ImportSchemaV1.getSchema();

                // 添加生成元数据
                Map<String, Object> response = new LinkedHashMap<>();
                response.putAll(schema);
                response.put("_meta", Map.of(
                                "generated_at", Instant.now().toString(),
                                "lang", lang));

                return ResponseEntity.ok(response);
        }

        /**
         * 获取导入模板
         * 
         * GET /api/import-format/template
         * 
         * @param version Schema 版本，目前仅支持 v1
         * @param mode    模板模式：empty | example_sql | example_rule
         * @param lang    语言，目前仅支持 zh
         * @return 可直接使用的 JSON 模板
         */
        @GetMapping("/template")
        public ResponseEntity<Map<String, Object>> getTemplate(
                        @RequestParam(defaultValue = "v1") String version,
                        @RequestParam(defaultValue = "empty") String mode,
                        @RequestParam(defaultValue = "zh") String lang) {

                log.info("获取导入模板: version={}, mode={}, lang={}", version, mode, lang);

                if (!"v1".equals(version)) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error_code", "UNSUPPORTED_VERSION",
                                        "message", "不支持的版本：" + version + "，当前仅支持 v1"));
                }

                Map<String, Object> template = ImportTemplateGenerator.generate(mode);

                // 添加生成元数据
                Map<String, Object> response = new LinkedHashMap<>();
                response.putAll(template);
                response.put("_meta", Map.of(
                                "schema_id", ImportSchemaV1.SCHEMA_ID,
                                "schema_version", ImportSchemaV1.SCHEMA_VERSION,
                                "mode", mode,
                                "generated_at", Instant.now().toString(),
                                "lang", lang));

                return ResponseEntity.ok(response);
        }

        /**
         * 获取支持的版本列表
         * 
         * GET /api/import-format/versions
         */
        @GetMapping("/versions")
        public ResponseEntity<Map<String, Object>> getVersions() {
                return ResponseEntity.ok(Map.of(
                                "current", "v1",
                                "supported", java.util.List.of("v1"),
                                "schemas", java.util.List.of(Map.of(
                                                "version", "v1",
                                                "schema_id", ImportSchemaV1.SCHEMA_ID,
                                                "schema_version", ImportSchemaV1.SCHEMA_VERSION,
                                                "doc_type", ImportSchemaV1.DOC_TYPE))));
        }

        /**
         * 获取提示词片段
         * 
         * GET /api/import-format/prompt-snippet
         * 
         * @param mode 模式：full | compact
         * @param lang 语言：zh | en
         * @return 可粘贴到提示词中的格式说明文本
         */
        @GetMapping("/prompt-snippet")
        public ResponseEntity<Map<String, Object>> getPromptSnippet(
                        @RequestParam(defaultValue = "full") String mode,
                        @RequestParam(defaultValue = "zh") String lang) {

                log.info("获取提示词片段: mode={}, lang={}", mode, lang);

                String snippet = "compact".equals(mode)
                                ? ImportPromptSnippetGenerator.generateCompact(lang)
                                : ImportPromptSnippetGenerator.generateFull(lang);

                return ResponseEntity.ok(Map.of(
                                "mode", mode,
                                "lang", lang,
                                "snippet", snippet,
                                "schema_version", ImportSchemaV1.SCHEMA_VERSION,
                                "generated_at", Instant.now().toString()));
        }

        /**
         * 校验导入 JSON
         * 
         * POST /api/import-format/validate
         * 
         * @param data 待校验的 JSON 数据
         * @return 校验结果
         */
        @PostMapping("/validate")
        public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> data) {
                log.info("校验导入数据");

                ImportSchemaValidator.ValidationResult result = validator.validate(data);

                if (result.valid()) {
                        return ResponseEntity.ok(Map.of(
                                        "valid", true,
                                        "schema_version", ImportSchemaV1.SCHEMA_VERSION));
                }

                // 转换 violations 为 Map 列表
                var violations = result.violations().stream()
                                .map(v -> Map.of(
                                                "path", v.path(),
                                                "reason", v.reason(),
                                                "value", v.value() != null ? v.value() : ""))
                                .toList();

                return ResponseEntity.badRequest().body(Map.of(
                                "valid", false,
                                "error_code", "IMPORT_SCHEMA_VALIDATION_FAILED",
                                "message", "Invalid import json",
                                "violations", violations));
        }
}
