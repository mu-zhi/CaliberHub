package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CanonicalEntityRelationPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanonicalEntityRelationMapper extends JpaRepository<CanonicalEntityRelationPO, Long> {

    List<CanonicalEntityRelationPO> findBySourceCanonicalEntityIdOrderByUpdatedAtDesc(Long sourceCanonicalEntityId);
}
