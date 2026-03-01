package com.cmbchina.datadirect.caliber.application.service.query;

import com.cmbchina.datadirect.caliber.application.api.dto.request.SceneListQuery;
import com.cmbchina.datadirect.caliber.application.assembler.SceneAssembler;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.domain.support.CaliberDomainSupport;
import com.cmbchina.datadirect.caliber.domain.support.SceneDomainSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SceneQueryAppServiceTest {

    @Mock
    private SceneDomainSupport sceneDomainSupport;

    @Mock
    private CaliberDomainSupport caliberDomainSupport;

    @Mock
    private SceneAssembler sceneAssembler;

    private SceneQueryAppService sceneQueryAppService;

    @BeforeEach
    void setUp() {
        sceneQueryAppService = new SceneQueryAppService(
                sceneDomainSupport,
                caliberDomainSupport,
                sceneAssembler,
                new ObjectMapper()
        );
    }

    @Test
    void shouldRejectInvalidStatus() {
        SceneListQuery query = new SceneListQuery(null, null, "INVALID", "客户");

        assertThatThrownBy(() -> sceneQueryAppService.list(query))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("allowed values");
    }
}
