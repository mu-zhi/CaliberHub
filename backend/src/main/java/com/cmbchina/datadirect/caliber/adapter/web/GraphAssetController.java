package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertCoverageDeclarationCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertContractViewCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertEvidenceFragmentCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertInputSlotSchemaCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertOutputContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertPlanCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertPolicyCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertSourceContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.request.graphrag.UpsertSourceIntakeContractCmd;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.CoverageDeclarationDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.ContractViewDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.EvidenceFragmentDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.InputSlotSchemaDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.OutputContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PlanDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PolicyDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.PublishCheckDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceContractDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.graphrag.SourceIntakeContractDTO;
import com.cmbchina.datadirect.caliber.application.service.command.graphrag.GraphAssetAppService;
import com.cmbchina.datadirect.caliber.application.service.query.SceneQueryAppService;
import com.cmbchina.datadirect.caliber.application.service.query.graphrag.ScenePublishGateAppService;
import com.cmbchina.datadirect.caliber.domain.model.Scene;
import com.cmbchina.datadirect.caliber.domain.model.SceneStatus;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.SecurityOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class GraphAssetController {

    private final GraphAssetAppService graphAssetAppService;
    private final SceneQueryAppService sceneQueryAppService;
    private final ScenePublishGateAppService scenePublishGateAppService;

    public GraphAssetController(GraphAssetAppService graphAssetAppService,
                                SceneQueryAppService sceneQueryAppService,
                                ScenePublishGateAppService scenePublishGateAppService) {
        this.graphAssetAppService = graphAssetAppService;
        this.sceneQueryAppService = sceneQueryAppService;
        this.scenePublishGateAppService = scenePublishGateAppService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> listPlans(@RequestParam(required = false) Long sceneId,
                                                   @RequestParam(required = false) Long domainId,
                                                   @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listPlans(sceneId, domainId, status));
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanDTO> createPlan(@RequestBody UpsertPlanCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertPlan(null, withOperator(cmd)));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<PlanDTO> updatePlan(@PathVariable Long id, @RequestBody UpsertPlanCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertPlan(id, withOperator(cmd)));
    }

    @GetMapping("/evidence-fragments")
    public ResponseEntity<List<EvidenceFragmentDTO>> listEvidence(@RequestParam(required = false) Long sceneId,
                                                                  @RequestParam(required = false) Long domainId,
                                                                  @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listEvidenceFragments(sceneId, domainId, status));
    }

    @PostMapping("/evidence-fragments")
    public ResponseEntity<EvidenceFragmentDTO> createEvidence(@RequestBody UpsertEvidenceFragmentCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertEvidenceFragment(null, withOperator(cmd)));
    }

    @PutMapping("/evidence-fragments/{id}")
    public ResponseEntity<EvidenceFragmentDTO> updateEvidence(@PathVariable Long id, @RequestBody UpsertEvidenceFragmentCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertEvidenceFragment(id, withOperator(cmd)));
    }

    @GetMapping("/coverage-declarations")
    public ResponseEntity<List<CoverageDeclarationDTO>> listCoverage(@RequestParam(required = false) Long sceneId,
                                                                     @RequestParam(required = false) Long domainId,
                                                                     @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listCoverageDeclarations(sceneId, domainId, status));
    }

    @PostMapping("/coverage-declarations")
    public ResponseEntity<CoverageDeclarationDTO> createCoverage(@RequestBody UpsertCoverageDeclarationCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertCoverageDeclaration(null, withOperator(cmd)));
    }

    @PutMapping("/coverage-declarations/{id}")
    public ResponseEntity<CoverageDeclarationDTO> updateCoverage(@PathVariable Long id, @RequestBody UpsertCoverageDeclarationCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertCoverageDeclaration(id, withOperator(cmd)));
    }

    @GetMapping("/policies")
    public ResponseEntity<List<PolicyDTO>> listPolicies(@RequestParam(required = false) Long sceneId,
                                                        @RequestParam(required = false) Long domainId,
                                                        @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listPolicies(sceneId, domainId, status));
    }

    @PostMapping("/policies")
    public ResponseEntity<PolicyDTO> createPolicy(@RequestBody UpsertPolicyCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertPolicy(null, withOperator(cmd)));
    }

    @PutMapping("/policies/{id}")
    public ResponseEntity<PolicyDTO> updatePolicy(@PathVariable Long id, @RequestBody UpsertPolicyCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertPolicy(id, withOperator(cmd)));
    }

    @GetMapping("/output-contracts")
    public ResponseEntity<List<OutputContractDTO>> listOutputContracts(@RequestParam(required = false) Long sceneId,
                                                                       @RequestParam(required = false) Long domainId,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listOutputContracts(sceneId, domainId, status));
    }

    @PostMapping("/output-contracts")
    public ResponseEntity<OutputContractDTO> createOutputContract(@RequestBody UpsertOutputContractCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertOutputContract(null, withOperator(cmd)));
    }

    @PutMapping("/output-contracts/{id}")
    public ResponseEntity<OutputContractDTO> updateOutputContract(@PathVariable Long id, @RequestBody UpsertOutputContractCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertOutputContract(id, withOperator(cmd)));
    }

    @GetMapping("/input-slot-schemas")
    public ResponseEntity<List<InputSlotSchemaDTO>> listInputSlots(@RequestParam(required = false) Long sceneId,
                                                                   @RequestParam(required = false) Long domainId,
                                                                   @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listInputSlotSchemas(sceneId, domainId, status));
    }

    @PostMapping("/input-slot-schemas")
    public ResponseEntity<InputSlotSchemaDTO> createInputSlot(@RequestBody UpsertInputSlotSchemaCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertInputSlotSchema(null, withOperator(cmd)));
    }

    @PutMapping("/input-slot-schemas/{id}")
    public ResponseEntity<InputSlotSchemaDTO> updateInputSlot(@PathVariable Long id, @RequestBody UpsertInputSlotSchemaCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertInputSlotSchema(id, withOperator(cmd)));
    }

    @GetMapping("/source-intake-contracts")
    public ResponseEntity<List<SourceIntakeContractDTO>> listSourceIntakes(@RequestParam(required = false) Long sceneId,
                                                                           @RequestParam(required = false) Long domainId,
                                                                           @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listSourceIntakeContracts(sceneId, domainId, status));
    }

    @PostMapping("/source-intake-contracts")
    public ResponseEntity<SourceIntakeContractDTO> createSourceIntake(@RequestBody UpsertSourceIntakeContractCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertSourceIntakeContract(null, withOperator(cmd)));
    }

    @PutMapping("/source-intake-contracts/{id}")
    public ResponseEntity<SourceIntakeContractDTO> updateSourceIntake(@PathVariable Long id, @RequestBody UpsertSourceIntakeContractCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertSourceIntakeContract(id, withOperator(cmd)));
    }

    @GetMapping("/contract-views")
    public ResponseEntity<List<ContractViewDTO>> listContractViews(@RequestParam(required = false) Long sceneId,
                                                                   @RequestParam(required = false) Long domainId,
                                                                   @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listContractViews(sceneId, domainId, status));
    }

    @PostMapping("/contract-views")
    public ResponseEntity<ContractViewDTO> createContractView(@RequestBody UpsertContractViewCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertContractView(null, withOperator(cmd)));
    }

    @PutMapping("/contract-views/{id}")
    public ResponseEntity<ContractViewDTO> updateContractView(@PathVariable Long id, @RequestBody UpsertContractViewCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertContractView(id, withOperator(cmd)));
    }

    @GetMapping("/source-contracts")
    public ResponseEntity<List<SourceContractDTO>> listSourceContracts(@RequestParam(required = false) Long sceneId,
                                                                       @RequestParam(required = false) Long domainId,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(graphAssetAppService.listSourceContracts(sceneId, domainId, status));
    }

    @PostMapping("/source-contracts")
    public ResponseEntity<SourceContractDTO> createSourceContract(@RequestBody UpsertSourceContractCmd cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(graphAssetAppService.upsertSourceContract(null, withOperator(cmd)));
    }

    @PutMapping("/source-contracts/{id}")
    public ResponseEntity<SourceContractDTO> updateSourceContract(@PathVariable Long id, @RequestBody UpsertSourceContractCmd cmd) {
        return ResponseEntity.ok(graphAssetAppService.upsertSourceContract(id, withOperator(cmd)));
    }

    @GetMapping("/publish-checks/{sceneId}")
    public ResponseEntity<PublishCheckDTO> publishCheck(@PathVariable Long sceneId) {
        return ResponseEntity.ok(scenePublishGateAppService.check(toScene(sceneQueryAppService.getById(sceneId))));
    }

    private UpsertPlanCmd withOperator(UpsertPlanCmd cmd) {
        return new UpsertPlanCmd(cmd.sceneId(), cmd.planCode(), cmd.planName(), cmd.applicablePeriod(), cmd.defaultTimeSemantic(), cmd.sourceTablesJson(), cmd.notes(), cmd.sqlText(), cmd.confidenceScore(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()), cmd.evidenceIds(), cmd.policyIds());
    }

    private UpsertEvidenceFragmentCmd withOperator(UpsertEvidenceFragmentCmd cmd) {
        return new UpsertEvidenceFragmentCmd(cmd.sceneId(), cmd.evidenceCode(), cmd.title(), cmd.fragmentText(), cmd.sourceAnchor(), cmd.sourceType(), cmd.sourceRef(), cmd.confidenceScore(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()), cmd.planIds());
    }

    private UpsertCoverageDeclarationCmd withOperator(UpsertCoverageDeclarationCmd cmd) {
        return new UpsertCoverageDeclarationCmd(cmd.planId(), cmd.coverageCode(), cmd.coverageTitle(), cmd.coverageType(), cmd.coverageStatus(), cmd.statementText(), cmd.applicablePeriod(), cmd.timeSemantic(), cmd.sourceSystem(), cmd.sourceTablesJson(), cmd.gapText(), cmd.active(), cmd.startDate(), cmd.endDate(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private UpsertPolicyCmd withOperator(UpsertPolicyCmd cmd) {
        return new UpsertPolicyCmd(cmd.policyCode(), cmd.policyName(), cmd.scopeType(), cmd.scopeRefId(), cmd.effectType(), cmd.conditionText(), cmd.sourceType(), cmd.sensitivityLevel(), cmd.maskingRule(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()), cmd.planIds());
    }

    private UpsertOutputContractCmd withOperator(UpsertOutputContractCmd cmd) {
        return new UpsertOutputContractCmd(cmd.sceneId(), cmd.contractCode(), cmd.contractName(), cmd.summaryText(), cmd.fieldsJson(), cmd.maskingRulesJson(), cmd.usageConstraints(), cmd.timeCaliberNote(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private UpsertInputSlotSchemaCmd withOperator(UpsertInputSlotSchemaCmd cmd) {
        return new UpsertInputSlotSchemaCmd(cmd.sceneId(), cmd.slotCode(), cmd.slotName(), cmd.slotType(), cmd.requiredFlag(), cmd.identifierCandidatesJson(), cmd.normalizationRule(), cmd.clarificationHint(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private UpsertSourceIntakeContractCmd withOperator(UpsertSourceIntakeContractCmd cmd) {
        return new UpsertSourceIntakeContractCmd(cmd.sceneId(), cmd.intakeCode(), cmd.intakeName(), cmd.sourceType(), cmd.requiredFieldsJson(), cmd.completenessRule(), cmd.gapTaskHint(), cmd.sourceTableHintsJson(), cmd.knownCoverageJson(), cmd.sensitivityLevel(), cmd.defaultTimeSemantic(), cmd.materialSourceNote(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private UpsertContractViewCmd withOperator(UpsertContractViewCmd cmd) {
        return new UpsertContractViewCmd(cmd.sceneId(), cmd.planId(), cmd.outputContractId(), cmd.viewCode(), cmd.viewName(), cmd.roleScope(), cmd.visibleFieldsJson(), cmd.maskedFieldsJson(), cmd.restrictedFieldsJson(), cmd.forbiddenFieldsJson(), cmd.approvalTemplate(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private UpsertSourceContractCmd withOperator(UpsertSourceContractCmd cmd) {
        return new UpsertSourceContractCmd(cmd.sceneId(), cmd.planId(), cmd.intakeContractId(), cmd.sourceContractCode(), cmd.sourceName(), cmd.physicalTable(), cmd.sourceRole(), cmd.identifierType(), cmd.outputIdentifierType(), cmd.sourceSystem(), cmd.timeSemantic(), cmd.completenessLevel(), cmd.sensitivityLevel(), cmd.startDate(), cmd.endDate(), cmd.materialSourceNote(), cmd.notes(), cmd.expectedVersion(), SecurityOperator.currentOperator(cmd.operator()));
    }

    private Scene toScene(com.cmbchina.datadirect.caliber.application.api.dto.response.SceneDTO dto) {
        return Scene.builder()
                .id(dto.id())
                .sceneCode(dto.sceneCode())
                .sceneTitle(dto.sceneTitle())
                .domainId(dto.domainId())
                .domain(dto.domain())
                .sceneType(dto.sceneType())
                .status(dto.status() == null || dto.status().isBlank() ? null : SceneStatus.valueOf(dto.status()))
                .sceneDescription(dto.sceneDescription())
                .caliberDefinition(dto.caliberDefinition())
                .applicability(dto.applicability())
                .boundaries(dto.boundaries())
                .inputsJson(dto.inputsJson())
                .outputsJson(dto.outputsJson())
                .sqlVariantsJson(dto.sqlVariantsJson())
                .codeMappingsJson(dto.codeMappingsJson())
                .contributors(dto.contributors())
                .sqlBlocksJson(dto.sqlBlocksJson())
                .sourceTablesJson(dto.sourceTablesJson())
                .caveatsJson(dto.caveatsJson())
                .unmappedText(dto.unmappedText())
                .qualityJson(dto.qualityJson())
                .rawInput(dto.rawInput())
                .verifiedAt(dto.verifiedAt())
                .changeSummary(dto.changeSummary())
                .createdBy(dto.createdBy())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .publishedBy(dto.publishedBy())
                .publishedAt(dto.publishedAt())
                .rowVersion(dto.rowVersion())
                .build();
    }
}
