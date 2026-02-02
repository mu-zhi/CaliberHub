package com.caliberhub.infrastructure.scene.dao.mapper;

import com.caliberhub.infrastructure.scene.dao.po.DomainPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 业务领域 Mapper
 */
@Repository
public interface DomainMapper extends JpaRepository<DomainPO, String> {
    
    Optional<DomainPO> findByDomainKey(String domainKey);
}
