package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class CanonicalKeyFactory {

    public Optional<String> buildSourceContractKey(SourceContractPO po) {
        if (po == null || isBlank(po.getSourceSystem()) || isBlank(po.getNormalizedPhysicalTable())) {
            return Optional.empty();
        }
        return Optional.of("SRC::" + normalize(po.getSourceSystem()) + "::" + normalize(po.getNormalizedPhysicalTable()));
    }

    public Optional<String> buildPolicyKey(PolicyPO po) {
        if (po == null || isBlank(po.getPolicySemanticKey())) {
            return Optional.empty();
        }
        return Optional.of("PLC::" + normalize(po.getPolicySemanticKey()));
    }

    public Optional<String> buildEvidenceKey(EvidenceFragmentPO po) {
        if (po == null || isBlank(po.getOriginType()) || isBlank(po.getOriginRef()) || isBlank(po.getOriginLocator())) {
            return Optional.empty();
        }
        return Optional.of("EVD::" + normalize(po.getOriginType())
                + "::" + normalize(po.getOriginRef())
                + "::" + normalize(po.getOriginLocator()));
    }

    public Optional<String> buildOutputContractKey(OutputContractPO po) {
        if (po == null || isBlank(po.getContractSemanticKey())) {
            return Optional.empty();
        }
        return Optional.of("OUT::" + normalize(po.getContractSemanticKey()));
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
