package com.cmbchina.datadirect.caliber.infrastructure.module.converter;

import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SceneConverter {

    ScenePO toPO(Scene scene);

    Scene toDomain(ScenePO scenePO);
}
