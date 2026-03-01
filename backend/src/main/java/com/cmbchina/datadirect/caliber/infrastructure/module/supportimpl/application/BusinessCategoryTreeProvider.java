package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BusinessCategoryTreeProvider {

    public static final String ROOT_COLUMN_ID = "ROOT";

    private static final Logger LOG = LoggerFactory.getLogger(BusinessCategoryTreeProvider.class);
    private static final String CATEGORY_PATH = "static/business_categories.json";

    private final ObjectMapper objectMapper;
    private volatile TopicTree tree = TopicTree.empty();

    public BusinessCategoryTreeProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.tree = loadTree();
    }

    public List<TopicNode> roots() {
        return toNodes(tree.rootIds(), tree.nodesById());
    }

    public List<TopicNode> childrenOf(String parentId) {
        return toNodes(tree.childrenByParent().getOrDefault(parentId, List.of()), tree.nodesById());
    }

    public boolean hasChildren(String nodeId) {
        return !tree.childrenByParent().getOrDefault(nodeId, List.of()).isEmpty();
    }

    public Optional<TopicNode> findById(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tree.nodesById().get(nodeId));
    }

    public List<TopicNode> leafNodes() {
        return toNodes(tree.leafIds(), tree.nodesById());
    }

    private TopicTree loadTree() {
        ClassPathResource resource = new ClassPathResource(CATEGORY_PATH);
        if (!resource.exists()) {
            LOG.warn("business category file is missing: {}", CATEGORY_PATH);
            return TopicTree.empty();
        }

        try (InputStream input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            JsonNode rootsNode = root.path("roots");
            if (!rootsNode.isArray()) {
                LOG.warn("invalid business category format: roots is not array");
                return TopicTree.empty();
            }

            Map<String, TopicNode> nodesById = new LinkedHashMap<>();
            Map<String, List<String>> childrenByParent = new LinkedHashMap<>();
            List<String> rootIds = new ArrayList<>();

            for (JsonNode item : rootsNode) {
                String rootId = trimOrEmpty(item.path("id").asText());
                if (rootId.isEmpty()) {
                    continue;
                }
                rootIds.add(rootId);
                parseNode(item, null, nodesById, childrenByParent);
            }

            List<String> leafIds = nodesById.values().stream()
                    .filter(node -> childrenByParent.getOrDefault(node.id(), List.of()).isEmpty())
                    .map(TopicNode::id)
                    .toList();

            return new TopicTree(
                    List.copyOf(rootIds),
                    Collections.unmodifiableMap(nodesById),
                    toImmutableChildren(childrenByParent),
                    List.copyOf(leafIds)
            );
        } catch (Exception ex) {
            LOG.warn("failed to load business category tree", ex);
            return TopicTree.empty();
        }
    }

    private Map<String, List<String>> toImmutableChildren(Map<String, List<String>> mutableMap) {
        Map<String, List<String>> immutable = new LinkedHashMap<>();
        mutableMap.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(immutable);
    }

    private void parseNode(JsonNode node,
                           String parentId,
                           Map<String, TopicNode> nodesById,
                           Map<String, List<String>> childrenByParent) {
        String id = trimOrEmpty(node.path("id").asText());
        if (id.isEmpty()) {
            return;
        }
        String code = trimOrEmpty(node.path("code").asText());
        String name = trimOrEmpty(node.path("name").asText());
        int level = node.path("level").asInt(0);

        nodesById.put(id, new TopicNode(id, parentId, code, name, level));

        if (parentId != null) {
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(id);
        }

        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            parseNode(child, id, nodesById, childrenByParent);
        }
    }

    private List<TopicNode> toNodes(List<String> ids, Map<String, TopicNode> nodesById) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<TopicNode> result = new ArrayList<>();
        for (String id : ids) {
            TopicNode node = nodesById.get(id);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public record TopicNode(
            String id,
            String parentId,
            String code,
            String name,
            int level
    ) {
    }

    private record TopicTree(
            List<String> rootIds,
            Map<String, TopicNode> nodesById,
            Map<String, List<String>> childrenByParent,
            List<String> leafIds
    ) {
        private static TopicTree empty() {
            return new TopicTree(List.of(), Map.of(), Map.of(), List.of());
        }
    }
}
