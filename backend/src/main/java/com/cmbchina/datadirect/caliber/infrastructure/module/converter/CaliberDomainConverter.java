package com.cmbchina.datadirect.caliber.infrastructure.module.converter;

import com.cmbchina.datadirect.caliber.domain.model.CaliberDomain;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.CaliberDomainPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CaliberDomainConverter {

    CaliberDomainPO toPO(CaliberDomain domain);

    CaliberDomain toDomain(CaliberDomainPO po);
}
