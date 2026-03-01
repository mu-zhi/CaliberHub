package com.cmbchina.datadirect.caliber.domain.support;

import com.cmbchina.datadirect.caliber.domain.model.LlmPreprocessConfig;

import java.util.Optional;

public interface LlmPreprocessConfigDomainSupport {

    Optional<LlmPreprocessConfig> findSingleton();

    LlmPreprocessConfig save(LlmPreprocessConfig config);
}
