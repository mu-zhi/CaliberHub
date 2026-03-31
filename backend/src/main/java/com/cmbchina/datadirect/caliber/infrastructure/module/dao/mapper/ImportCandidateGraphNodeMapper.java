package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphNodePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportCandidateGraphNodeMapper extends JpaRepository<ImportCandidateGraphNodePO, Long> {

    List<ImportCandidateGraphNodePO> findByTaskId(String taskId);

    List<ImportCandidateGraphNodePO> findByTaskIdAndGraphId(String taskId, String graphId);

    Optional<ImportCandidateGraphNodePO> findByNodeCode(String nodeCode);

    Optional<ImportCandidateGraphNodePO> findByTaskIdAndNodeCode(String taskId, String nodeCode);

    List<ImportCandidateGraphNodePO> findByTaskIdAndSceneCandidateCodeAndReviewStatus(String taskId, String sceneCandidateCode, String reviewStatus);

    void deleteByTaskId(String taskId);
}
