package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportSceneCandidatePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface ImportSceneCandidateMapper extends JpaRepository<ImportSceneCandidatePO, Long> {

    List<ImportSceneCandidatePO> findByTaskId(String taskId);

    List<ImportSceneCandidatePO> findByTaskIdAndMaterialId(String taskId, String materialId);

    Optional<ImportSceneCandidatePO> findByCandidateCode(String candidateCode);

    void deleteByTaskId(String taskId);
}
