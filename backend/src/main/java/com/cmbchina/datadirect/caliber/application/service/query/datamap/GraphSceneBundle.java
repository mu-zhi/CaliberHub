package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.ContractViewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.CoverageDeclarationDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.EvidenceFragmentDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.OutputContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PlanDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PolicyDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceIntakeContractDTO;

import java.util.List;

public record GraphSceneBundle(
        SceneDTO scene,
        List<PlanDTO> plans,
        List<OutputContractDTO> outputContracts,
        List<ContractViewDTO> contractViews,
        List<CoverageDeclarationDTO> coverages,
        List<PolicyDTO> policies,
        List<EvidenceFragmentDTO> evidences,
        List<SourceContractDTO> sourceContracts,
        List<SourceIntakeContractDTO> sourceIntakeContracts
) {
}
