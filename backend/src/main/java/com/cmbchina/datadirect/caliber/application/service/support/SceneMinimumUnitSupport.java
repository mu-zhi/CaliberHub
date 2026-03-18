package com.cmbchina.datadirect.caliber.application.service.support;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitCheckItemDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneMinimumUnitDefinitionDTO;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SceneMinimumUnitSupport {

    public static final String UNIT_TYPE = "CALIBER_SCENE_UNIT_V1";
    public static final String SCHEMA_VERSION = "1.0.0";

    private SceneMinimumUnitSupport() {
    }

    public static SceneMinimumUnitDefinitionDTO definition() {
        return new SceneMinimumUnitDefinitionDTO(
                UNIT_TYPE,
                SCHEMA_VERSION,
                List.of("sceneTitle", "domainId", "sceneDescription", "sqlVariants[].sql_text"),
                "用于发布门禁的最小单元结构：标题、业务领域、场景描述、至少一条可执行查询方案。"
        );
    }

    public static SceneMinimumUnitCheckDTO check(Scene scene, ObjectMapper objectMapper) {
        if (scene == null) {
            return new SceneMinimumUnitCheckDTO(
                    null,
                    UNIT_TYPE,
                    SCHEMA_VERSION,
                    false,
                    List.of(
                            new SceneMinimumUnitCheckItemDTO("scene", "场景实体", false, "场景不存在")
                    )
            );
        }
        List<SceneMinimumUnitCheckItemDTO> items = new ArrayList<>();
        items.add(item(
                "scene_title",
                "场景标题",
                isNotBlank(scene.getSceneTitle()),
                "场景标题不能为空"
        ));
        items.add(item(
                "domain_id",
                "业务领域",
                scene.getDomainId() != null,
                "发布前必须绑定业务领域（domainId）"
        ));
        items.add(item(
                "scene_description",
                "场景描述",
                isNotBlank(scene.getSceneDescription()),
                "场景描述不能为空"
        ));
        items.add(item(
                "sql_variant",
                "查询方案",
                hasRunnableSql(scene, objectMapper),
                "至少需要一条包含 SQL 的查询方案"
        ));
        boolean ready = items.stream().allMatch(it -> Boolean.TRUE.equals(it.passed()));
        return new SceneMinimumUnitCheckDTO(
                scene.getId(),
                UNIT_TYPE,
                SCHEMA_VERSION,
                ready,
                items
        );
    }

    private static SceneMinimumUnitCheckItemDTO item(String key, String name, boolean passed, String failedMessage) {
        return new SceneMinimumUnitCheckItemDTO(
                key,
                name,
                passed,
                passed ? "已满足" : failedMessage
        );
    }

    private static boolean hasRunnableSql(Scene scene, ObjectMapper objectMapper) {
        String raw = firstNotBlank(scene.getSqlVariantsJson(), scene.getSqlBlocksJson());
        if (!isNotBlank(raw)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isArray()) {
                return false;
            }
            for (JsonNode node : root) {
                String sql = text(node.path("sql_text"));
                if (sql.isBlank()) {
                    sql = text(node.path("sql"));
                }
                if (!sql.isBlank() && sql.toLowerCase(Locale.ROOT).contains("select")) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            return false;
        }
        return false;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static String firstNotBlank(String first, String second) {
        if (isNotBlank(first)) {
            return first;
        }
        if (isNotBlank(second)) {
            return second;
        }
        return "";
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
