package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.CaliberDictMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDictPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class CaliberDictSyncService {

    private static final String GLOBAL_SCOPE = "GLOBAL";

    private final CaliberDictMapper caliberDictMapper;
    private final ObjectMapper objectMapper;

    public CaliberDictSyncService(CaliberDictMapper caliberDictMapper, ObjectMapper objectMapper) {
        this.caliberDictMapper = caliberDictMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncFromScene(Long sceneId, Long domainId, String codeMappingsJson) {
        if (sceneId == null || codeMappingsJson == null || codeMappingsJson.isBlank()) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(codeMappingsJson);
        } catch (Exception ex) {
            return;
        }
        if (!root.isArray()) {
            return;
        }

        String domainScope = domainId == null ? GLOBAL_SCOPE : "DOMAIN_" + domainId;
        for (JsonNode mappingNode : root) {
            String code = trimToEmpty(mappingNode.path("code").asText(""));
            if (code.isBlank()) {
                continue;
            }
            String description = trimToEmpty(mappingNode.path("description").asText(""));
            JsonNode mappingsNode = mappingNode.path("mappings");
            if (!mappingsNode.isObject()) {
                continue;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = mappingsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String valueCode = trimToEmpty(entry.getKey());
                String valueName = trimToEmpty(entry.getValue() == null ? "" : entry.getValue().asText(""));
                if (valueCode.isBlank()) {
                    continue;
                }
                upsert(domainScope, domainId, code, valueCode, valueName, description, sceneId);
            }
        }
    }

    private void upsert(String domainScope,
                        Long domainId,
                        String code,
                        String valueCode,
                        String valueName,
                        String description,
                        Long sceneId) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<CaliberDictPO> existing = caliberDictMapper.findByDomainScopeAndCodeAndValueCode(domainScope, code, valueCode);
        CaliberDictPO po = existing.orElseGet(CaliberDictPO::new);
        if (po.getId() == null) {
            po.setCreatedAt(now);
        }
        po.setDomainScope(domainScope);
        po.setDomainId(domainId);
        po.setCode(code);
        po.setValueCode(valueCode);
        po.setValueName(valueName);
        po.setDescription(description);
        po.setLastSceneId(sceneId);
        po.setUpdatedAt(now);
        caliberDictMapper.save(po);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
