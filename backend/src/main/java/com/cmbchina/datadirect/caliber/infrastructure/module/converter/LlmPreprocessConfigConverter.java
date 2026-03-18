package com.cmbchina.datadirect.caliber.infrastructure.module.converter;

import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.LlmPreprocessConfigPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LlmPreprocessConfigConverter {

    LlmPreprocessConfigPO toPO(LlmPreprocessConfig domain);

    LlmPreprocessConfig toDomain(LlmPreprocessConfigPO po);
}
