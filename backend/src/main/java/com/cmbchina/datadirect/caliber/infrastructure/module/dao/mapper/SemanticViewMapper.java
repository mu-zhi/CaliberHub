package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.SemanticViewPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemanticViewMapper extends JpaRepository<SemanticViewPO, Long> {

    Optional<SemanticViewPO> findByViewCode(String viewCode);

    boolean existsByViewCode(String viewCode);
}

