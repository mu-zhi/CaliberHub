package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportEvidenceCandidatePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportEvidenceCandidateMapper extends JpaRepository<ImportEvidenceCandidatePO, Long> {

    List<ImportEvidenceCandidatePO> findByTaskId(String taskId);

    List<ImportEvidenceCandidatePO> findByTaskIdAndMaterialId(String taskId, String materialId);

    List<ImportEvidenceCandidatePO> findBySceneCandidateCode(String sceneCandidateCode);

    void deleteByTaskId(String taskId);
}
