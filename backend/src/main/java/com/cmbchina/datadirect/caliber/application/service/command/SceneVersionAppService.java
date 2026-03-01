package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.application.api.dto.request.CreateSceneVersionCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDiffDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneVersionDTO;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneVersionMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SceneVersionPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class SceneVersionAppService {

    private final SceneMapper sceneMapper;
    private final SceneVersionMapper sceneVersionMapper;
    private final ObjectMapper objectMapper;

    public SceneVersionAppService(SceneMapper sceneMapper,
                                  SceneVersionMapper sceneVersionMapper,
                                  ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.sceneVersionMapper = sceneVersionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SceneVersionDTO create(Long sceneId, CreateSceneVersionCmd cmd) {
        ScenePO scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("scene not found: " + sceneId));
        int nextVersion = sceneVersionMapper.findTopBySceneIdOrderByVersionNoDesc(sceneId)
                .map(item -> item.getVersionNo() + 1)
                .orElse(1);
        SceneVersionPO po = new SceneVersionPO();
        po.setSceneId(sceneId);
        po.setVersionNo(nextVersion);
        po.setSnapshotJson(writeJson(scene));
        po.setChangeSummary(cmd.changeSummary());
        po.setCreatedBy(cmd.operator());
        po.setCreatedAt(OffsetDateTime.now());
        return toDTO(sceneVersionMapper.save(po));
    }

    public List<SceneVersionDTO> list(Long sceneId) {
        return sceneVersionMapper.findBySceneIdOrderByVersionNoDesc(sceneId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public SceneDiffDTO diff(Long sceneId, Integer from, Integer to) {
        SceneVersionPO left = sceneVersionMapper.findBySceneIdAndVersionNo(sceneId, from)
                .orElseThrow(() -> new ResourceNotFoundException("scene version not found: " + sceneId + "#" + from));
        SceneVersionPO right = sceneVersionMapper.findBySceneIdAndVersionNo(sceneId, to)
                .orElseThrow(() -> new ResourceNotFoundException("scene version not found: " + sceneId + "#" + to));
        JsonNode leftJson = readJson(left.getSnapshotJson());
        JsonNode rightJson = readJson(right.getSnapshotJson());
        List<String> changed = new ArrayList<>();
        Iterator<String> fields = rightJson.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            JsonNode leftValue = leftJson.path(field);
            JsonNode rightValue = rightJson.path(field);
            if (!leftValue.equals(rightValue)) {
                changed.add(field);
            }
        }
        return new SceneDiffDTO(sceneId, from, to, changed);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("invalid version snapshot json", ex);
        }
    }

    private String writeJson(ScenePO scene) {
        try {
            return objectMapper.writeValueAsString(scene);
        } catch (Exception ex) {
            throw new IllegalStateException("write scene snapshot failed", ex);
        }
    }

    private SceneVersionDTO toDTO(SceneVersionPO po) {
        return new SceneVersionDTO(
                po.getId(),
                po.getSceneId(),
                po.getVersionNo(),
                po.getSnapshotJson(),
                po.getChangeSummary(),
                po.getCreatedBy(),
                po.getCreatedAt()
        );
    }
}
