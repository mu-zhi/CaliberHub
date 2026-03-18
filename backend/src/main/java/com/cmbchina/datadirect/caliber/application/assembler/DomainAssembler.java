package com.cmbchina.datadirect.caliber.application.assembler;

import com.cmbchina.datadirect.caliber.application.api.dto.response.DomainDTO;
import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DomainAssembler {

    DomainDTO toDTO(CaliberDomain domain);

    List<DomainDTO> toDTOList(List<CaliberDomain> domains);
}
