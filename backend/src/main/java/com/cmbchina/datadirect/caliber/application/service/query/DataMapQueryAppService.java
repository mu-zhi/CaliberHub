package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.api.dto.response.BusinessDomainDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.FetchColumnResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LineageEdgeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LineageGraphDataDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.LineageNodeDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.MillerNodeDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.service.support.SqlTableParseSupport;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application.BusinessCategoryTreeProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DataMapQueryAppService {

    private static final String DOMAIN_COLUMN_PREFIX = "domain:";
    private static final String UNCLASSIFIED_TOPIC_ID = "domain:unclassified";
    private static final String UNCLASSIFIED_TOPIC_LABEL = "未归属业务领域";
    private final SceneQueryAppService sceneQueryAppService;
    private final CaliberDomainSupport caliberDomainSupport;
    private final BusinessCategoryTreeProvider businessCategoryTreeProvider;
    private final ObjectMapper objectMapper;

    public DataMapQueryAppService(SceneQueryAppService sceneQueryAppService,
                                  CaliberDomainSupport caliberDomainSupport,
                                  BusinessCategoryTreeProvider businessCategoryTreeProvider,
                                  ObjectMapper objectMapper) {
        this.sceneQueryAppService = sceneQueryAppService;
        this.caliberDomainSupport = caliberDomainSupport;
        this.businessCategoryTreeProvider = businessCategoryTreeProvider;
        this.objectMapper = objectMapper;
    }

    public FetchColumnResponseDTO fetchColumn(String columnId, String keyword, String view) {
        String normalizedColumnId = trimOrEmpty(columnId);
        if (normalizedColumnId.isEmpty()) {
            throw new DomainValidationException("columnId is required");
        }

        String normalizedKeyword = trimOrNull(keyword);
        List<SceneDTO> scenes = listScenes(normalizedKeyword);
        Map<Long, List<SceneDTO>> scenesByDomain = groupScenesByDomain(scenes);
        List<CaliberDomain> domains = listDomains();
        List<SceneDTO> unclassifiedScenes = scenes.stream()
                .filter(scene -> scene.domainId() == null || scene.domainId() <= 0)
                .toList();

        if (BusinessCategoryTreeProvider.ROOT_COLUMN_ID.equalsIgnoreCase(normalizedColumnId)) {
            return new FetchColumnResponseDTO(
                    BusinessCategoryTreeProvider.ROOT_COLUMN_ID,
                    buildDomainRootNodes(domains, scenesByDomain, unclassifiedScenes, normalizedKeyword, view)
            );
        }

        if (UNCLASSIFIED_TOPIC_ID.equals(normalizedColumnId)) {
            return new FetchColumnResponseDTO(
                    UNCLASSIFIED_TOPIC_ID,
                    buildSceneNodes(unclassifiedScenes)
            );
        }

        if (normalizedColumnId.startsWith(DOMAIN_COLUMN_PREFIX)) {
            Long domainId = parseDomainColumnId(normalizedColumnId);
            if (domainId != null) {
                List<SceneDTO> domainScenes = scenesByDomain.getOrDefault(domainId, List.of());
                return new FetchColumnResponseDTO(normalizedColumnId, buildSceneNodes(domainScenes));
            }
        }

        // 兼容历史 topic id，避免旧链接直接失效
        SceneGrouping grouping = groupScenesByLeafTopic(scenes);
        BusinessCategoryTreeProvider.TopicNode topic = businessCategoryTreeProvider.findById(normalizedColumnId)
                .orElseThrow(() -> new ResourceNotFoundException("topic node not found: " + normalizedColumnId));

        List<BusinessCategoryTreeProvider.TopicNode> children = businessCategoryTreeProvider.childrenOf(topic.id());
        if (!children.isEmpty()) {
            return new FetchColumnResponseDTO(topic.id(), buildTopicNodes(children, grouping));
        }

        List<SceneDTO> scenesInLeaf = grouping.scenesByLeaf().getOrDefault(topic.id(), List.of());
        return new FetchColumnResponseDTO(topic.id(), buildSceneNodes(scenesInLeaf));
    }

    public LineageGraphDataDTO fetchLineage(Long sceneId, Integer maxNodes) {
        if (sceneId == null || sceneId <= 0) {
            throw new DomainValidationException("sceneId must be positive");
        }
        int safeMaxNodes = normalizeMaxNodes(maxNodes);

        SceneDTO scene = sceneQueryAppService.getById(sceneId);
        List<String> sourceTables = new ArrayList<>(extractSourceTables(scene));
        List<String> targetTables = new ArrayList<>(extractTargetTables(scene));
        if (sourceTables.isEmpty()) {
            sourceTables.add("UNKNOWN_SOURCE_TABLE");
        }
        if (targetTables.isEmpty()) {
            targetTables.add("UNRESOLVED_TARGET_TABLE");
        }

        List<String> visibleSources = new ArrayList<>(sourceTables);
        List<String> visibleTargets = new ArrayList<>(targetTables);
        int fullNodeCount = sourceTables.size() + targetTables.size() + 2;

        while (visibleSources.size() + visibleTargets.size() + 2 > safeMaxNodes) {
            if (visibleTargets.size() >= visibleSources.size() && visibleTargets.size() > 1) {
                visibleTargets.remove(visibleTargets.size() - 1);
                continue;
            }
            if (visibleSources.size() > 1) {
                visibleSources.remove(visibleSources.size() - 1);
                continue;
            }
            if (visibleTargets.size() > 1) {
                visibleTargets.remove(visibleTargets.size() - 1);
                continue;
            }
            break;
        }

        String warehouseNodeId = "warehouse:" + scene.id();
        String warehouseLabel = resolveWarehouseLabel(scene);
        String appNodeId = "scene:" + scene.id();
        String appLabel = trimOrEmpty(scene.sceneTitle()).isEmpty() ? ("场景" + scene.id()) : scene.sceneTitle();

        List<LineageNodeDTO> nodes = new ArrayList<>();
        List<LineageEdgeDTO> edges = new ArrayList<>();

        Map<String, String> sourceNodeIds = new LinkedHashMap<>();
        for (String table : visibleSources) {
            String nodeId = nodeId("source", table);
            sourceNodeIds.put(table, nodeId);
            nodes.add(new LineageNodeDTO(nodeId, table, "SOURCE", "ACTIVE"));
        }
        nodes.add(new LineageNodeDTO(warehouseNodeId, warehouseLabel, "WAREHOUSE", "ACTIVE"));

        Map<String, String> targetNodeIds = new LinkedHashMap<>();
        for (String table : visibleTargets) {
            String nodeId = nodeId("mart", table);
            targetNodeIds.put(table, nodeId);
            nodes.add(new LineageNodeDTO(nodeId, table, "MART", "ACTIVE"));
        }
        nodes.add(new LineageNodeDTO(appNodeId, appLabel, "APP", trimOrEmpty(scene.status())));

        for (String nodeId : sourceNodeIds.values()) {
            edges.add(new LineageEdgeDTO(nodeId, warehouseNodeId, "读取"));
        }
        for (String nodeId : targetNodeIds.values()) {
            edges.add(new LineageEdgeDTO(warehouseNodeId, nodeId, "加工"));
            edges.add(new LineageEdgeDTO(nodeId, appNodeId, "服务"));
        }
        if (targetNodeIds.isEmpty()) {
            edges.add(new LineageEdgeDTO(warehouseNodeId, appNodeId, "输出"));
        }

        int hiddenNodeCount = Math.max(0, fullNodeCount - nodes.size());
        return new LineageGraphDataDTO(nodes, edges, hiddenNodeCount > 0, hiddenNodeCount);
    }

    private List<CaliberDomain> listDomains() {
        return caliberDomainSupport.findAllOrderBySortOrder();
    }

    private List<MillerNodeDTO> buildDomainRootNodes(List<CaliberDomain> domains,
                                                     Map<Long, List<SceneDTO>> scenesByDomain,
                                                     List<SceneDTO> unclassifiedScenes,
                                                     String keyword,
                                                     String view) {
        List<MillerNodeDTO> rootNodes = new ArrayList<>();
        for (CaliberDomain domain : domains) {
            Long domainId = domain.getId();
            if (domainId == null || domainId <= 0) {
                continue;
            }
            int sceneCount = scenesByDomain.getOrDefault(domainId, List.of()).size();
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("domainId", domainId);
            meta.put("domainCode", trimOrEmpty(domain.getDomainCode()));
            meta.put("domainName", trimOrEmpty(domain.getDomainName()));
            meta.put("sceneCount", sceneCount);
            if (keyword != null) {
                meta.put("keyword", keyword);
            }
            if (view != null && !view.isBlank()) {
                meta.put("view", view);
            }

            String domainLabel = trimOrEmpty(domain.getDomainName());
            if (domainLabel.isEmpty()) {
                domainLabel = trimOrEmpty(domain.getDomainCode());
            }
            if (domainLabel.isEmpty()) {
                domainLabel = "业务领域#" + domainId;
            }

            rootNodes.add(new MillerNodeDTO(
                    DOMAIN_COLUMN_PREFIX + domainId,
                    BusinessCategoryTreeProvider.ROOT_COLUMN_ID,
                    domainLabel,
                    "ROOT",
                    true,
                    null,
                    Collections.unmodifiableMap(meta)
            ));
        }

        if (!unclassifiedScenes.isEmpty()) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("bucket", "unclassified");
            meta.put("sceneCount", unclassifiedScenes.size());
            if (keyword != null) {
                meta.put("keyword", keyword);
            }
            if (view != null && !view.isBlank()) {
                meta.put("view", view);
            }
            rootNodes.add(new MillerNodeDTO(
                    UNCLASSIFIED_TOPIC_ID,
                    BusinessCategoryTreeProvider.ROOT_COLUMN_ID,
                    UNCLASSIFIED_TOPIC_LABEL,
                    "ROOT",
                    true,
                    null,
                    Collections.unmodifiableMap(meta)
            ));
        }
        return rootNodes;
    }

    private Map<Long, List<SceneDTO>> groupScenesByDomain(List<SceneDTO> scenes) {
        Map<Long, List<SceneDTO>> grouped = new LinkedHashMap<>();
        for (SceneDTO scene : scenes) {
            Long domainId = scene.domainId();
            if (domainId == null || domainId <= 0) {
                continue;
            }
            grouped.computeIfAbsent(domainId, ignored -> new ArrayList<>()).add(scene);
        }
        Map<Long, List<SceneDTO>> immutable = new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(immutable);
    }

    private Long parseDomainColumnId(String columnId) {
        String safe = trimOrEmpty(columnId);
        if (!safe.startsWith(DOMAIN_COLUMN_PREFIX)) {
            return null;
        }
        String idText = safe.substring(DOMAIN_COLUMN_PREFIX.length()).trim();
        if (idText.isEmpty() || "unclassified".equalsIgnoreCase(idText)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(idText);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<MillerNodeDTO> buildRootNodes(SceneGrouping grouping, String keyword, String view) {
        List<BusinessCategoryTreeProvider.TopicNode> roots = businessCategoryTreeProvider.roots();
        List<MillerNodeDTO> rootNodes = buildTopicNodes(roots, grouping);
        if (!grouping.unclassifiedScenes().isEmpty()) {
            rootNodes = new ArrayList<>(rootNodes);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("bucket", "unclassified");
            meta.put("sceneCount", grouping.unclassifiedScenes().size());
            if (keyword != null) {
                meta.put("keyword", keyword);
            }
            if (view != null && !view.isBlank()) {
                meta.put("view", view);
            }
            rootNodes.add(new MillerNodeDTO(
                    UNCLASSIFIED_TOPIC_ID,
                    BusinessCategoryTreeProvider.ROOT_COLUMN_ID,
                    UNCLASSIFIED_TOPIC_LABEL,
                    "TOPIC",
                    true,
                    null,
                    Collections.unmodifiableMap(meta)
            ));
        }
        return rootNodes;
    }

    private List<MillerNodeDTO> buildTopicNodes(List<BusinessCategoryTreeProvider.TopicNode> topics, SceneGrouping grouping) {
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }
        List<MillerNodeDTO> result = new ArrayList<>();
        for (BusinessCategoryTreeProvider.TopicNode topic : topics) {
            int sceneCount = countScenesInSubtree(topic.id(), grouping.scenesByLeaf());
            boolean hasChildren = businessCategoryTreeProvider.hasChildren(topic.id()) || sceneCount > 0;

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("code", topic.code());
            meta.put("level", topic.level());
            meta.put("sceneCount", sceneCount);

            result.add(new MillerNodeDTO(
                    topic.id(),
                    topic.parentId(),
                    topic.name(),
                    topic.level() <= 1 ? "ROOT" : "TOPIC",
                    hasChildren,
                    null,
                    Collections.unmodifiableMap(meta)
            ));
        }
        return result;
    }

    private List<MillerNodeDTO> buildSceneNodes(List<SceneDTO> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return List.of();
        }
        List<MillerNodeDTO> result = new ArrayList<>();
        for (SceneDTO scene : scenes) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("sceneId", scene.id());
            meta.put("domainId", scene.domainId());
            meta.put("domainName", trimOrEmpty(scene.domainName()));
            meta.put("updatedAt", scene.updatedAt() == null ? null : scene.updatedAt().toString());

            String label = trimOrEmpty(scene.sceneTitle());
            if (label.isEmpty()) {
                label = "场景" + scene.id();
            }
            result.add(new MillerNodeDTO(
                    "scene:" + scene.id(),
                    null,
                    label,
                    "SCENE",
                    false,
                    trimOrEmpty(scene.status()),
                    Collections.unmodifiableMap(meta)
            ));
        }
        return result;
    }

    private int countScenesInSubtree(String topicId, Map<String, List<SceneDTO>> scenesByLeaf) {
        List<BusinessCategoryTreeProvider.TopicNode> children = businessCategoryTreeProvider.childrenOf(topicId);
        if (children.isEmpty()) {
            return scenesByLeaf.getOrDefault(topicId, List.of()).size();
        }
        int total = 0;
        for (BusinessCategoryTreeProvider.TopicNode child : children) {
            total += countScenesInSubtree(child.id(), scenesByLeaf);
        }
        return total;
    }

    private SceneGrouping groupScenesByLeafTopic(List<SceneDTO> scenes) {
        List<BusinessCategoryTreeProvider.TopicNode> leafTopics = businessCategoryTreeProvider.leafNodes();
        Map<String, List<SceneDTO>> grouped = new LinkedHashMap<>();
        List<SceneDTO> unclassified = new ArrayList<>();
        for (SceneDTO scene : scenes) {
            Optional<String> matchedTopicId = pickBestLeafTopic(scene, leafTopics);
            if (matchedTopicId.isPresent()) {
                grouped.computeIfAbsent(matchedTopicId.get(), ignored -> new ArrayList<>()).add(scene);
            } else {
                unclassified.add(scene);
            }
        }
        return new SceneGrouping(toImmutableGroup(grouped), List.copyOf(unclassified));
    }

    private Map<String, List<SceneDTO>> toImmutableGroup(Map<String, List<SceneDTO>> grouped) {
        Map<String, List<SceneDTO>> immutable = new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(immutable);
    }

    private Optional<String> pickBestLeafTopic(SceneDTO scene, List<BusinessCategoryTreeProvider.TopicNode> leafTopics) {
        if (leafTopics == null || leafTopics.isEmpty()) {
            return Optional.empty();
        }
        String domainName = normalizeText(scene.domainName());
        String domain = normalizeText(scene.domain());
        String title = normalizeText(scene.sceneTitle());
        String description = normalizeText(scene.sceneDescription());

        int bestScore = 0;
        String bestTopicId = null;
        for (BusinessCategoryTreeProvider.TopicNode topic : leafTopics) {
            String topicName = normalizeText(topic.name());
            String topicCode = normalizeText(topic.code());
            int score = 0;

            score += containsScore(domainName, topicName, 30);
            score += containsScore(domain, topicName, 24);
            score += containsScore(title, topicName, 20);
            score += containsScore(description, topicName, 8);
            score += containsScore(title, topicCode, 6);

            if (score > bestScore) {
                bestScore = score;
                bestTopicId = topic.id();
            }
        }
        if (bestScore <= 0 || bestTopicId == null) {
            return Optional.empty();
        }
        return Optional.of(bestTopicId);
    }

    private int containsScore(String left, String right, int baseScore) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        if (left.contains(right)) {
            return baseScore;
        }
        if (right.length() >= 3 && right.contains(left)) {
            return Math.max(2, baseScore / 3);
        }
        return 0;
    }

    private List<SceneDTO> listScenes(String keyword) {
        return sceneQueryAppService.list(new SceneListQuery(null, null, null, keyword));
    }

    private Set<String> extractSourceTables(SceneDTO scene) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        tables.addAll(readStringArray(scene.sourceTablesJson()));
        for (JsonNode variant : parseVariants(scene)) {
            JsonNode sourceTablesNode = variant.path("source_tables");
            if (sourceTablesNode.isArray()) {
                for (JsonNode source : sourceTablesNode) {
                    String table = trimOrEmpty(source.asText());
                    if (!table.isEmpty()) {
                        tables.add(table);
                    }
                }
            }
            String sqlText = trimOrEmpty(variant.path("sql_text").asText());
            SqlTableParseSupport.ParseResult parsed = SqlTableParseSupport.parse(sqlText);
            tables.addAll(parsed.sourceTables());
        }
        if (tables.isEmpty()) {
            String rawSql = trimOrEmpty(scene.rawInput());
            SqlTableParseSupport.ParseResult parsed = SqlTableParseSupport.parse(rawSql);
            tables.addAll(parsed.sourceTables());
        }
        return tables;
    }

    private Set<String> extractTargetTables(SceneDTO scene) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        for (JsonNode variant : parseVariants(scene)) {
            String sqlText = trimOrEmpty(variant.path("sql_text").asText());
            SqlTableParseSupport.ParseResult parsed = SqlTableParseSupport.parse(sqlText);
            tables.addAll(parsed.targetTables());
        }
        if (tables.isEmpty()) {
            String rawSql = trimOrEmpty(scene.rawInput());
            SqlTableParseSupport.ParseResult parsed = SqlTableParseSupport.parse(rawSql);
            tables.addAll(parsed.targetTables());
        }
        return tables;
    }

    private List<JsonNode> parseVariants(SceneDTO scene) {
        JsonNode variants = readJsonNode(scene.sqlVariantsJson());
        if (variants != null && variants.isArray()) {
            return toNodeList(variants);
        }
        JsonNode fallback = readJsonNode(scene.sqlBlocksJson());
        if (fallback != null && fallback.isArray()) {
            return toNodeList(fallback);
        }
        return List.of();
    }

    private List<JsonNode> toNodeList(JsonNode arrayNode) {
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            result.add(item);
        }
        return result;
    }

    private JsonNode readJsonNode(String raw) {
        String safe = trimOrEmpty(raw);
        if (safe.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(safe);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> readStringArray(String raw) {
        String safe = trimOrEmpty(raw);
        if (safe.isEmpty()) {
            return List.of();
        }
        JsonNode node = readJsonNode(safe);
        if (node != null && node.isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                String text = trimOrEmpty(item.asText());
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
            return result;
        }

        String[] split = safe.split("[,，;；\\n]+");
        List<String> result = new ArrayList<>();
        for (String item : split) {
            String text = trimOrEmpty(item);
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private String resolveWarehouseLabel(SceneDTO scene) {
        for (JsonNode variant : parseVariants(scene)) {
            String name = trimOrEmpty(variant.path("variant_name").asText());
            if (!name.isEmpty()) {
                return "ETL · " + name;
            }
        }
        String code = trimOrEmpty(scene.sceneCode());
        if (!code.isEmpty()) {
            return "ETL · " + code;
        }
        return "ETL 作业";
    }

    private int normalizeMaxNodes(Integer maxNodes) {
        int value = (maxNodes == null ? 50 : maxNodes);
        if (value < 4) {
            return 4;
        }
        return Math.min(value, 200);
    }

    private String nodeId(String prefix, String rawText) {
        String safe = trimOrEmpty(rawText).toLowerCase(Locale.ROOT);
        String compact = safe.replaceAll("[^a-z0-9_.$]+", "_");
        if (compact.isBlank()) {
            compact = "node";
        }
        if (compact.length() > 42) {
            compact = compact.substring(0, 42);
        }
        return prefix + ":" + compact + ":" + Integer.toHexString(safe.hashCode());
    }

    private String normalizeText(String text) {
        return trimOrEmpty(text).toLowerCase(Locale.ROOT);
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimOrNull(String value) {
        String safe = trimOrEmpty(value);
        return safe.isEmpty() ? null : safe;
    }

    public List<BusinessDomainDTO> listBusinessDomains() {
        return listDomains().stream()
                .filter(domain -> domain.getId() != null && domain.getId() > 0)
                .map(domain -> new BusinessDomainDTO(
                        domain.getId(),
                        trimOrEmpty(domain.getDomainCode()),
                        trimOrEmpty(domain.getDomainName())
                ))
                .toList();
    }

    private record SceneGrouping(
            Map<String, List<SceneDTO>> scenesByLeaf,
            List<SceneDTO> unclassifiedScenes
    ) {
    }
}
