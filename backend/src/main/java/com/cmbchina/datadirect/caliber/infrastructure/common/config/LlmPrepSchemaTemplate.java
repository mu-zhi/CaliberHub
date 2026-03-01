package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import java.util.List;
import java.util.Map;

public final class LlmPrepSchemaTemplate {

    private LlmPrepSchemaTemplate() {
    }

    public static PrepSchema sample() {
        return new PrepSchema(
                "CALIBER_PREP_V1",
                "1.2.0",
                "PASTE_MD|FILE_MD|FILE_TXT|FILE_SQL|IMAGE_OCR_TEXT",
                new DocProfile("zh|en|mixed|unknown", true, 1, "LOW|MEDIUM|HIGH", false),
                new Context("string", "string"),
                "string",
                List.of(
                        new SceneCandidate("S001", "string", "string", List.of("SQL_001"), 0.0, List.of(1, 2, 3))
                ),
                List.of(
                        new SqlSegment(
                                "SQL_001",
                                "string",
                                "string",
                                "string",
                                "SELECT|WITH|INSERT|UPDATE|DELETE|DDL|UNKNOWN",
                                true,
                                List.of(new SourceSpan(1, 20)),
                                List.of()
                        )
                ),
                List.of(
                        new TableHint("schema.table", "SQL_001", 0.0)
                ),
                List.of(
                        new FieldHint("field_name", "schema.table", "string", Map.of("code_value", "code_description"), 0.0)
                ),
                List.of(
                        new RiskNote("string", "string", "LOW|MEDIUM|HIGH", List.of(10, 11))
                ),
                "string",
                List.of("string"),
                new Quality(0.0, List.of("string"), List.of("string"))
        );
    }

    public record PrepSchema(
            String prep_type,
            String schema_version,
            String source_type,
            DocProfile doc_profile,
            Context context,
            String normalized_text,
            List<SceneCandidate> scene_candidates,
            List<SqlSegment> sql_segments,
            List<TableHint> table_hints,
            List<FieldHint> field_hints,
            List<RiskNote> risk_notes,
            String carry_over_text,
            List<String> unresolved,
            Quality quality
    ) {
    }

    public record DocProfile(
            String language,
            boolean has_sql,
            int estimated_scene_count,
            String ocr_noise_level,
            boolean is_truncated
    ) {
    }

    public record Context(
            String domain_guess,
            String document_title
    ) {
    }

    public record SceneCandidate(
            String scene_id,
            String scene_title,
            String scene_description_hint,
            List<String> sql_segment_ids,
            double confidence,
            List<Integer> evidence_lines
    ) {
    }

    public record SqlSegment(
            String segment_id,
            String name_hint,
            String applicable_period,
            String sql_raw,
            String sql_type,
            boolean is_complete,
            List<SourceSpan> source_spans,
            List<String> warnings
    ) {
    }

    public record SourceSpan(
            int start_line,
            int end_line
    ) {
    }

    public record TableHint(
            String table,
            String from_segment_id,
            double confidence
    ) {
    }

    public record FieldHint(
            String field,
            String table,
            String meaning_hint,
            Map<String, String> extracted_mappings,
            double confidence
    ) {
    }

    public record RiskNote(
            String title,
            String text,
            String risk,
            List<Integer> evidence_lines
    ) {
    }

    public record Quality(
            double confidence,
            List<String> warnings,
            List<String> errors
    ) {
    }
}
