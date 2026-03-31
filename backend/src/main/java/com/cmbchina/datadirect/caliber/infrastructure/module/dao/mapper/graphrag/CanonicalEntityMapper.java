package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanonicalEntityMapper extends JpaRepository<CanonicalEntityPO, Long> {

    Optional<CanonicalEntityPO> findByEntityTypeAndCanonicalKey(String entityType, String canonicalKey);
}
