package com.cmbchina.datadirect.caliber.application.assembler;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SceneAssembler {

    @Mapping(target = "domainName", source = "domain")
    @Mapping(target = "snapshotId", expression = "java(null)")
    SceneDTO toDTO(Scene scene);

    List<SceneDTO> toDTOList(List<Scene> scenes);
}
