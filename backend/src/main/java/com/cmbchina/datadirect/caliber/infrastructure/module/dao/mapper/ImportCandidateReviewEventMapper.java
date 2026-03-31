package com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ImportCandidateReviewEventPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportCandidateReviewEventMapper extends JpaRepository<ImportCandidateReviewEventPO, Long> {

    List<ImportCandidateReviewEventPO> findByTaskIdAndGraphIdOrderByCreatedAtAsc(String taskId, String graphId);
}
