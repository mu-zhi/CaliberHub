package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateGraphEdgePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportCandidateGraphEdgeMapper extends JpaRepository<ImportCandidateGraphEdgePO, Long> {

    List<ImportCandidateGraphEdgePO> findByTaskId(String taskId);

    List<ImportCandidateGraphEdgePO> findByTaskIdAndGraphId(String taskId, String graphId);

    Optional<ImportCandidateGraphEdgePO> findByEdgeCode(String edgeCode);

    Optional<ImportCandidateGraphEdgePO> findByTaskIdAndEdgeCode(String taskId, String edgeCode);

    void deleteByTaskId(String taskId);
}
