package com.cmbchina.datadirect.caliber.application.service.command;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.CaliberDictMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDictPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaliberDictSyncServiceTest {

    @Mock
    private CaliberDictMapper caliberDictMapper;

    @Test
    void shouldUpsertMappingsFromSceneJson() {
        CaliberDictSyncService service = new CaliberDictSyncService(caliberDictMapper, new ObjectMapper());
        when(caliberDictMapper.findByDomainScopeAndCodeAndValueCode(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        String codeMappingsJson = """
                [
                  {
                    "code": "CARD_GRD_CD",
                    "description": "卡等级",
                    "mappings": {
                      "1": "普卡",
                      "2": "金卡"
                    }
                  }
                ]
                """;
        service.syncFromScene(101L, 9L, codeMappingsJson);

        ArgumentCaptor<CaliberDictPO> captor = ArgumentCaptor.forClass(CaliberDictPO.class);
        verify(caliberDictMapper, times(2)).save(captor.capture());
        CaliberDictPO saved = captor.getAllValues().get(0);
        assertThat(saved.getDomainScope()).isEqualTo("DOMAIN_9");
        assertThat(saved.getCode()).isEqualTo("CARD_GRD_CD");
        assertThat(saved.getLastSceneId()).isEqualTo(101L);
    }
}
