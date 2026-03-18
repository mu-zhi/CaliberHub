package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ServiceSpecPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceSpecMapper extends JpaRepository<ServiceSpecPO, Long> {

    Optional<ServiceSpecPO> findTopBySceneIdOrderBySpecVersionDesc(Long sceneId);

    Optional<ServiceSpecPO> findTopBySpecCodeOrderBySpecVersionDesc(String specCode);

    Optional<ServiceSpecPO> findBySpecCodeAndSpecVersion(String specCode, Integer specVersion);

    List<ServiceSpecPO> findBySceneIdOrderBySpecVersionDesc(Long sceneId);

    long countBySceneId(Long sceneId);
}

