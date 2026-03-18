package com.cmbchina.datadirect.caliber.application.api.dto.response;

import java.util.List;

public record SceneDiffDTO(
        Long sceneId,
        Integer fromVersion,
        Integer toVersion,
        List<String> changedFields
) {
}

