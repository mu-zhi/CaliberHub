package com.cmbchina.datadirect.caliber.infrastructure.module.supportimpl.domain;

import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;
import com.cmbchina.datadirect.caliber.domain.support.LlmPreprocessConfigDomainSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.converter.LlmPreprocessConfigConverter;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.LlmPreprocessConfigMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.LlmPreprocessConfigPO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LlmPreprocessConfigDomainSupportImpl implements LlmPreprocessConfigDomainSupport {

    public static final long SINGLETON_ID = 1L;

    private final LlmPreprocessConfigMapper llmPreprocessConfigMapper;
    private final LlmPreprocessConfigConverter llmPreprocessConfigConverter;

    public LlmPreprocessConfigDomainSupportImpl(LlmPreprocessConfigMapper llmPreprocessConfigMapper,
                                                LlmPreprocessConfigConverter llmPreprocessConfigConverter) {
        this.llmPreprocessConfigMapper = llmPreprocessConfigMapper;
        this.llmPreprocessConfigConverter = llmPreprocessConfigConverter;
    }

    @Override
    public Optional<LlmPreprocessConfig> findSingleton() {
        return llmPreprocessConfigMapper.findById(SINGLETON_ID)
                .map(llmPreprocessConfigConverter::toDomain);
    }

    @Override
    public LlmPreprocessConfig save(LlmPreprocessConfig config) {
        LlmPreprocessConfigPO po = llmPreprocessConfigConverter.toPO(config);
        if (po.getId() == null) {
            po.setId(SINGLETON_ID);
        }
        LlmPreprocessConfigPO saved = llmPreprocessConfigMapper.save(po);
        return llmPreprocessConfigConverter.toDomain(saved);
    }
}
