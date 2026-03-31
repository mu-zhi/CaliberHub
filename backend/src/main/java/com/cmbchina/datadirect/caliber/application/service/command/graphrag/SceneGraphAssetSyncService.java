package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import com.cmbchina.datadirect.caliber.application.service.graphrag.GraphAssetSupport;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.SceneMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.CoverageDeclarationMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.ContractViewMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EntityAliasMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.EvidenceFragmentMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.InputSlotSchemaMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.OutputContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanEvidenceRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanPolicyRefMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PlanSchemaLinkMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.PolicyMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.mapper.graphrag.SourceIntakeContractMapper;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.ScenePO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.CoverageDeclarationPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.ContractViewPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EntityAliasPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.EvidenceFragmentPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.InputSlotSchemaPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.OutputContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanEvidenceRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanPolicyRefPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PlanSchemaLinkPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.PolicyPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceContractPO;
import com.cmbchina.datadirect.caliber.infrastructure.module.dao.po.graphrag.SourceIntakeContractPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SceneGraphAssetSyncService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private final PlanMapper planMapper;
    private final EvidenceFragmentMapper evidenceFragmentMapper;
    private final CoverageDeclarationMapper coverageDeclarationMapper;
    private final PolicyMapper policyMapper;
    private final PlanEvidenceRefMapper planEvidenceRefMapper;
    private final PlanPolicyRefMapper planPolicyRefMapper;
    private final EntityAliasMapper entityAliasMapper;
    private final PlanSchemaLinkMapper planSchemaLinkMapper;
    private final OutputContractMapper outputContractMapper;
    private final InputSlotSchemaMapper inputSlotSchemaMapper;
    private final SourceIntakeContractMapper sourceIntakeContractMapper;
    private final ContractViewMapper contractViewMapper;
    private final SourceContractMapper sourceContractMapper;
    private final SceneMapper sceneMapper;
    private final GraphAssetSupport graphAssetSupport;
    private final CanonicalEntityResolutionService canonicalEntityResolutionService;

    @Autowired
    public SceneGraphAssetSyncService(PlanMapper planMapper,
                                      EvidenceFragmentMapper evidenceFragmentMapper,
                                      CoverageDeclarationMapper coverageDeclarationMapper,
                                      PolicyMapper policyMapper,
                                      PlanEvidenceRefMapper planEvidenceRefMapper,
                                      PlanPolicyRefMapper planPolicyRefMapper,
                                      EntityAliasMapper entityAliasMapper,
                                      PlanSchemaLinkMapper planSchemaLinkMapper,
                                      OutputContractMapper outputContractMapper,
                                      InputSlotSchemaMapper inputSlotSchemaMapper,
                                      SourceIntakeContractMapper sourceIntakeContractMapper,
                                      ContractViewMapper contractViewMapper,
                                      SourceContractMapper sourceContractMapper,
                                      SceneMapper sceneMapper,
                                      GraphAssetSupport graphAssetSupport,
                                      CanonicalEntityResolutionService canonicalEntityResolutionService) {
        this.planMapper = planMapper;
        this.evidenceFragmentMapper = evidenceFragmentMapper;
        this.coverageDeclarationMapper = coverageDeclarationMapper;
        this.policyMapper = policyMapper;
        this.planEvidenceRefMapper = planEvidenceRefMapper;
        this.planPolicyRefMapper = planPolicyRefMapper;
        this.entityAliasMapper = entityAliasMapper;
        this.planSchemaLinkMapper = planSchemaLinkMapper;
        this.outputContractMapper = outputContractMapper;
        this.inputSlotSchemaMapper = inputSlotSchemaMapper;
        this.sourceIntakeContractMapper = sourceIntakeContractMapper;
        this.contractViewMapper = contractViewMapper;
        this.sourceContractMapper = sourceContractMapper;
        this.sceneMapper = sceneMapper;
        this.graphAssetSupport = graphAssetSupport;
        this.canonicalEntityResolutionService = canonicalEntityResolutionService;
    }

    public SceneGraphAssetSyncService(PlanMapper planMapper,
                                      EvidenceFragmentMapper evidenceFragmentMapper,
                                      CoverageDeclarationMapper coverageDeclarationMapper,
                                      PolicyMapper policyMapper,
                                      PlanEvidenceRefMapper planEvidenceRefMapper,
                                      PlanPolicyRefMapper planPolicyRefMapper,
                                      EntityAliasMapper entityAliasMapper,
                                      PlanSchemaLinkMapper planSchemaLinkMapper,
                                      OutputContractMapper outputContractMapper,
                                      InputSlotSchemaMapper inputSlotSchemaMapper,
                                      SourceIntakeContractMapper sourceIntakeContractMapper,
                                      ContractViewMapper contractViewMapper,
                                      SourceContractMapper sourceContractMapper,
                                      SceneMapper sceneMapper,
                                      GraphAssetSupport graphAssetSupport) {
        this(planMapper,
                evidenceFragmentMapper,
                coverageDeclarationMapper,
                policyMapper,
                planEvidenceRefMapper,
                planPolicyRefMapper,
                entityAliasMapper,
                planSchemaLinkMapper,
                outputContractMapper,
                inputSlotSchemaMapper,
                sourceIntakeContractMapper,
                contractViewMapper,
                sourceContractMapper,
                sceneMapper,
                graphAssetSupport,
                null);
    }

    @Transactional
    public void ensureGovernanceAssets(Long sceneId, String operator) {
        ScenePO scene = sceneMapper.findById(sceneId).orElse(null);
        if (scene == null) {
            return;
        }
        boolean missingPlans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        boolean missingOutputs = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        boolean missingSlots = inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        boolean missingIntake = sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        boolean missingContractViews = contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        boolean missingSourceContracts = sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).isEmpty();
        if (missingPlans || missingOutputs || missingSlots || missingIntake) {
            syncSceneAssetsFromLegacy(scene, operator);
        } else {
            String status = safeStatus(scene);
            String normalizedOperator = normalizeOperator(operator);
            OffsetDateTime now = OffsetDateTime.now();
            ensureDerivedPublishAssets(scene, status, normalizedOperator, now, missingContractViews, missingSourceContracts);
            ensureSceneLevelAliases(scene, status, normalizedOperator, now);
        }
    }

    @Transactional
    public void syncSceneAssetsFromLegacy(Long sceneId, String operator) {
        ScenePO scene = sceneMapper.findById(sceneId).orElse(null);
        if (scene == null) {
            return;
        }
        syncSceneAssetsFromLegacy(scene, operator);
    }

    @Transactional
    public void syncSceneAssetsFromLegacy(ScenePO scene, String operator) {
        if (scene == null || scene.getId() == null) {
            return;
        }
        deleteSceneScopedAssets(scene.getId());

        String status = safeStatus(scene);
        String normalizedOperator = normalizeOperator(operator);
        OffsetDateTime now = OffsetDateTime.now();

        syncGovernanceContracts(scene, status, normalizedOperator, now);
        syncPlansFromLegacy(scene, status, normalizedOperator, now);
        ensureDerivedPublishAssets(scene, status, normalizedOperator, now, true, true);
        ensureSceneLevelAliases(scene, status, normalizedOperator, now);
        syncSceneLegacyFields(scene.getId(), normalizedOperator);
        if (canonicalEntityResolutionService != null) {
            canonicalEntityResolutionService.resolveScene(scene.getId(), normalizedOperator);
        }
    }

    @Transactional
    public void syncSceneLegacyFields(Long sceneId, String operator) {
        ScenePO scene = sceneMapper.findById(sceneId).orElse(null);
        if (scene == null) {
            return;
        }
        List<PlanPO> plans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        ArrayNode variants = graphAssetSupport.parseArray("[]");
        LinkedHashSet<String> sourceTables = new LinkedHashSet<>();
        for (PlanPO plan : plans) {
            ObjectNode item = variants.addObject();
            item.put("variant_name", plan.getPlanName());
            item.put("applicable_period", Optional.ofNullable(plan.getApplicablePeriod()).orElse(""));
            item.put("sql_text", Optional.ofNullable(plan.getSqlText()).orElse(""));
            item.put("notes", Optional.ofNullable(plan.getNotes()).orElse(""));
            item.put("default_time_semantic", Optional.ofNullable(plan.getDefaultTimeSemantic()).orElse(""));
            ArrayNode tableNode = item.putArray("source_tables");
            for (String table : graphAssetSupport.parseStringList(plan.getSourceTablesJson())) {
                tableNode.add(table);
                sourceTables.add(table);
            }
        }

        List<OutputContractPO> contracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        if (!contracts.isEmpty()) {
            OutputContractPO primary = contracts.get(0);
            ObjectNode outputs = (ObjectNode) graphAssetSupport.parseJson("{}", "{}");
            outputs.put("summary", Optional.ofNullable(primary.getSummaryText()).orElse(""));
            outputs.set("fields", graphAssetSupport.parseJson(primary.getFieldsJson(), "[]"));
            if (primary.getMaskingRulesJson() != null && !primary.getMaskingRulesJson().isBlank()) {
                outputs.set("maskingRules", graphAssetSupport.parseJson(primary.getMaskingRulesJson(), "[]"));
            }
            scene.setOutputsJson(graphAssetSupport.writeJson(outputs, "{}"));
        }

        List<InputSlotSchemaPO> slots = inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        ObjectNode inputs = (ObjectNode) graphAssetSupport.parseJson("{}", "{}");
        ArrayNode params = inputs.putArray("params");
        for (InputSlotSchemaPO slot : slots) {
            ObjectNode item = params.addObject();
            item.put("name", slot.getSlotName());
            item.put("type", Optional.ofNullable(slot.getSlotType()).orElse("TEXT"));
            item.put("required", slot.isRequiredFlag());
            item.set("identifiers", graphAssetSupport.parseJson(slot.getIdentifierCandidatesJson(), "[]"));
            item.put("normalization_rule", Optional.ofNullable(slot.getNormalizationRule()).orElse(""));
            item.put("clarification_hint", Optional.ofNullable(slot.getClarificationHint()).orElse(""));
        }
        inputs.putArray("constraints");
        scene.setInputsJson(graphAssetSupport.writeJson(inputs, "{}"));
        scene.setSqlVariantsJson(graphAssetSupport.writeJson(variants, "[]"));
        scene.setSqlBlocksJson(graphAssetSupport.writeJson(variants, "[]"));
        scene.setSourceTablesJson(graphAssetSupport.writeJson(new ArrayList<>(sourceTables), "[]"));
        scene.setUpdatedAt(OffsetDateTime.now());
        sceneMapper.save(scene);
    }

    @Transactional
    public void syncAssetStatuses(Long sceneId, String sceneStatus, String operator) {
        String normalizedOperator = normalizeOperator(operator);
        String status = safeAssetStatus(sceneStatus);
        String policyStatus = "PUBLISHED".equals(status) ? "ACTIVE" : ("DISCARDED".equals(status) ? "INACTIVE" : "DRAFT");
        OffsetDateTime now = OffsetDateTime.now();

        outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(contract -> {
            contract.setStatus(status);
            contract.setUpdatedBy(normalizedOperator);
            contract.setUpdatedAt(now);
            outputContractMapper.save(contract);
        });
        inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(slot -> {
            slot.setStatus(status);
            slot.setUpdatedBy(normalizedOperator);
            slot.setUpdatedAt(now);
            inputSlotSchemaMapper.save(slot);
        });
        sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(intake -> {
            intake.setStatus(status);
            intake.setUpdatedBy(normalizedOperator);
            intake.setUpdatedAt(now);
            sourceIntakeContractMapper.save(intake);
        });
        contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(view -> {
            view.setStatus(status);
            view.setUpdatedBy(normalizedOperator);
            view.setUpdatedAt(now);
            contractViewMapper.save(view);
        });
        sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(contract -> {
            if (!"DEPRECATED".equalsIgnoreCase(contract.getStatus())) {
                contract.setStatus(status);
            }
            contract.setUpdatedBy(normalizedOperator);
            contract.setUpdatedAt(now);
            sourceContractMapper.save(contract);
        });
        evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(evidence -> {
            evidence.setStatus(status);
            evidence.setUpdatedBy(normalizedOperator);
            evidence.setUpdatedAt(now);
            evidenceFragmentMapper.save(evidence);
        });
        entityAliasMapper.findBySceneIdAndStatus(sceneId, "DRAFT").forEach(alias -> {
            alias.setStatus(status);
            alias.setUpdatedBy(normalizedOperator);
            alias.setUpdatedAt(now);
            entityAliasMapper.save(alias);
        });
        entityAliasMapper.findBySceneIdAndStatus(sceneId, "PUBLISHED").forEach(alias -> {
            alias.setStatus(status);
            alias.setUpdatedBy(normalizedOperator);
            alias.setUpdatedAt(now);
            entityAliasMapper.save(alias);
        });

        for (PlanPO plan : planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId)) {
            plan.setStatus(status);
            plan.setUpdatedBy(normalizedOperator);
            plan.setUpdatedAt(now);
            planMapper.save(plan);
            for (CoverageDeclarationPO coverage : coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId())) {
                coverage.setStatus(status);
                coverage.setUpdatedBy(normalizedOperator);
                coverage.setUpdatedAt(now);
                coverageDeclarationMapper.save(coverage);
            }
            for (PlanSchemaLinkPO schemaLink : planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "DRAFT")) {
                schemaLink.setStatus(status);
                schemaLink.setUpdatedBy(normalizedOperator);
                schemaLink.setUpdatedAt(now);
                planSchemaLinkMapper.save(schemaLink);
            }
            for (PlanSchemaLinkPO schemaLink : planSchemaLinkMapper.findByPlanIdAndStatus(plan.getId(), "PUBLISHED")) {
                schemaLink.setStatus(status);
                schemaLink.setUpdatedBy(normalizedOperator);
                schemaLink.setUpdatedAt(now);
                planSchemaLinkMapper.save(schemaLink);
            }
        }

        policyMapper.findAll().stream()
                .filter(policy -> appliesToScene(sceneId, policy))
                .forEach(policy -> {
                    policy.setStatus(policyStatus);
                    policy.setUpdatedBy(normalizedOperator);
                    policy.setUpdatedAt(now);
                    policyMapper.save(policy);
                });
    }

    @Transactional
    public void rebuildPlanDerivedAssets(Long planId, String operator) {
        PlanPO plan = planMapper.findById(planId).orElse(null);
        if (plan == null) {
            return;
        }
        ScenePO scene = sceneMapper.findById(plan.getSceneId()).orElse(null);
        if (scene == null) {
            return;
        }
        String status = safeAssetStatus(plan.getStatus());
        String normalizedOperator = normalizeOperator(operator);
        OffsetDateTime now = OffsetDateTime.now();

        entityAliasMapper.deleteByPlanId(planId);
        planSchemaLinkMapper.deleteByPlanId(planId);
        createAlias(scene.getId(), plan.getId(), plan.getPlanCode(), plan.getPlanName(), "PLAN", status, normalizedOperator, now);
        for (String table : graphAssetSupport.parseStringList(plan.getSourceTablesJson())) {
            createSchemaLink(plan.getId(), table, null, "JOIN", status, normalizedOperator, now);
        }
        for (String field : readOutputFields(scene.getId())) {
            createSchemaLink(plan.getId(), "SCENE_OUTPUT", field, "OUTPUT", status, normalizedOperator, now);
        }
    }

    private void syncGovernanceContracts(ScenePO scene, String status, String operator, OffsetDateTime now) {
        outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).forEach(outputContractMapper::delete);
        inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).forEach(inputSlotSchemaMapper::delete);
        sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).forEach(sourceIntakeContractMapper::delete);

        OutputContractPO outputContract = new OutputContractPO();
        outputContract.setSceneId(scene.getId());
        outputContract.setContractCode(scene.getSceneCode() + "-OUT-01");
        outputContract.setContractName(scene.getSceneTitle() + "输出契约");
        outputContract.setContractSemanticKey(scene.getSceneCode() + "::OUTPUT::PRIMARY");
        outputContract.setSummaryText(readOutputSummary(scene.getOutputsJson()));
        outputContract.setFieldsJson(readOutputFieldsJson(scene.getOutputsJson()));
        outputContract.setMaskingRulesJson("[]");
        outputContract.setUsageConstraints(Optional.ofNullable(scene.getBoundaries()).orElse(""));
        outputContract.setTimeCaliberNote(Optional.ofNullable(scene.getApplicability()).orElse(""));
        outputContract.setStatus(status);
        outputContract.setCreatedBy(operator);
        outputContract.setCreatedAt(now);
        outputContract.setUpdatedBy(operator);
        outputContract.setUpdatedAt(now);
        outputContractMapper.save(outputContract);

        List<InputSlotSchemaPO> slots = buildInputSlots(scene, status, operator, now);
        slots.forEach(inputSlotSchemaMapper::save);

        ArrayNode knownCoverage = graphAssetSupport.parseArray("[]");
        ArrayNode sourceTableHints = graphAssetSupport.parseArray("[]");
        LinkedHashSet<String> hintedTables = new LinkedHashSet<>(graphAssetSupport.parseStringList(scene.getSourceTablesJson()));
        ArrayNode variants = graphAssetSupport.parseArray(
                scene.getSqlVariantsJson() == null || scene.getSqlVariantsJson().isBlank()
                        ? scene.getSqlBlocksJson()
                        : scene.getSqlVariantsJson()
        );
        for (JsonNode variant : variants) {
            String applicablePeriod = graphAssetSupport.safeText(variant.path("applicable_period"));
            if (!applicablePeriod.isBlank()) {
                knownCoverage.add(applicablePeriod);
            }
            hintedTables.addAll(readSourceTables(variant.path("source_tables"), scene.getSourceTablesJson()));
            String sqlText = readSqlText(variant);
            if (hintedTables.isEmpty()) {
                hintedTables.addAll(inferSourceTablesFromText(sqlText));
            }
        }
        hintedTables.forEach(sourceTableHints::add);

        SourceIntakeContractPO intake = new SourceIntakeContractPO();
        intake.setSceneId(scene.getId());
        intake.setIntakeCode(scene.getSceneCode() + "-SRC-01");
        intake.setIntakeName(scene.getSceneTitle() + "资料接入契约");
        intake.setSourceType(scene.getRawInput() == null || scene.getRawInput().isBlank() ? "MANUAL_SCENE" : "RAW_INPUT_DOC");
        intake.setRequiredFieldsJson(graphAssetSupport.writeJson(List.of("sceneTitle", "sceneDescription", "planSqlOrEvidence"), "[]"));
        intake.setCompletenessRule("发布前需具备场景标题、场景描述、至少一条方案和至少一条证据。缺失资料进入缺口任务。");
        intake.setGapTaskHint("若资料缺失，请补齐原始口径文档、历史时段说明或字段语义说明。");
        intake.setSourceTableHintsJson(graphAssetSupport.writeJson(sourceTableHints, "[]"));
        intake.setKnownCoverageJson(graphAssetSupport.writeJson(knownCoverage, "[]"));
        intake.setSensitivityLevel("S1");
        intake.setDefaultTimeSemantic(inferDefaultTimeSemantic(scene, null, hintedTables, scene.getRawInput()));
        intake.setMaterialSourceNote(scene.getRawInput() == null || scene.getRawInput().isBlank() ? "无原始材料正文" : "导入原始材料已挂载到场景草稿");
        intake.setStatus(status);
        intake.setCreatedBy(operator);
        intake.setCreatedAt(now);
        intake.setUpdatedBy(operator);
        intake.setUpdatedAt(now);
        sourceIntakeContractMapper.save(intake);
    }

    private List<InputSlotSchemaPO> buildInputSlots(ScenePO scene, String status, String operator, OffsetDateTime now) {
        List<InputSlotSchemaPO> slots = new ArrayList<>();
        JsonNode root = graphAssetSupport.parseJson(scene.getInputsJson(), "{}");
        JsonNode params = root.path("params");
        int index = 0;
        if (params.isArray()) {
            for (JsonNode param : params) {
                String slotName = graphAssetSupport.safeText(param.path("name"));
                if (slotName.isBlank()) {
                    continue;
                }
                index += 1;
                InputSlotSchemaPO slot = new InputSlotSchemaPO();
                slot.setSceneId(scene.getId());
                slot.setSlotCode(scene.getSceneCode() + "-SLOT-" + String.format(Locale.ROOT, "%02d", index));
                slot.setSlotName(slotName);
                slot.setSlotType(Optional.ofNullable(graphAssetSupport.safeText(param.path("type"))).filter(text -> !text.isBlank()).orElse("TEXT"));
                slot.setRequiredFlag(param.path("required").asBoolean(false));
                slot.setIdentifierCandidatesJson(graphAssetSupport.writeJson(param.path("identifiers").isMissingNode() ? List.of(slotName) : param.path("identifiers"), "[]"));
                slot.setNormalizationRule(graphAssetSupport.safeText(param.path("normalization_rule")));
                slot.setClarificationHint(graphAssetSupport.safeText(param.path("clarification_hint")));
                slot.setStatus(status);
                slot.setCreatedBy(operator);
                slot.setCreatedAt(now);
                slot.setUpdatedBy(operator);
                slot.setUpdatedAt(now);
                slots.add(slot);
            }
        }
        if (!slots.isEmpty()) {
            return slots;
        }

        LinkedHashSet<String> inferred = new LinkedHashSet<>();
        inferPlaceholders(scene.getRawInput(), inferred);
        inferPlaceholders(scene.getSqlVariantsJson(), inferred);
        inferPlaceholders(scene.getSqlBlocksJson(), inferred);
        for (String name : inferred) {
            index += 1;
            InputSlotSchemaPO slot = new InputSlotSchemaPO();
            slot.setSceneId(scene.getId());
            slot.setSlotCode(scene.getSceneCode() + "-SLOT-" + String.format(Locale.ROOT, "%02d", index));
            slot.setSlotName(name);
            slot.setSlotType("TEXT");
            slot.setRequiredFlag(true);
            slot.setIdentifierCandidatesJson(graphAssetSupport.writeJson(List.of(name), "[]"));
            slot.setNormalizationRule("trim + upper_snake_case");
            slot.setClarificationHint("请补充 " + name + " 的标准标识或取值范围");
            slot.setStatus(status);
            slot.setCreatedBy(operator);
            slot.setCreatedAt(now);
            slot.setUpdatedBy(operator);
            slot.setUpdatedAt(now);
            slots.add(slot);
        }
        return slots;
    }

    private void syncPlansFromLegacy(ScenePO scene, String status, String operator, OffsetDateTime now) {
        ArrayNode variants = graphAssetSupport.parseArray(
                scene.getSqlVariantsJson() == null || scene.getSqlVariantsJson().isBlank()
                        ? scene.getSqlBlocksJson()
                        : scene.getSqlVariantsJson()
        );
        PolicyPO defaultPolicy = ensureDefaultPolicy(scene, operator, now);
        List<String> outputFields = readOutputFields(scene.getId());
        int index = 0;
        for (JsonNode variant : variants) {
            index += 1;
            String planName = graphAssetSupport.safeText(variant.path("variant_name"));
            if (planName.isBlank()) {
                planName = "取数方案" + index;
            }
            String applicablePeriod = graphAssetSupport.safeText(variant.path("applicable_period"));
            String sqlText = readSqlText(variant);
            List<String> sourceTables = readSourceTables(variant.path("source_tables"), scene.getSourceTablesJson());
            if (sourceTables.isEmpty()) {
                sourceTables = inferSourceTablesFromText(sqlText);
            }
            String notes = graphAssetSupport.safeText(variant.path("notes"));
            String defaultTimeSemantic = graphAssetSupport.safeText(variant.path("default_time_semantic"));
            if (defaultTimeSemantic.isBlank()) {
                defaultTimeSemantic = inferDefaultTimeSemantic(scene, planName, sourceTables, sqlText);
            }

            PlanPO plan = new PlanPO();
            plan.setSceneId(scene.getId());
            plan.setPlanCode(scene.getSceneCode() + "-PLN-" + String.format(Locale.ROOT, "%02d", index));
            plan.setPlanName(planName);
            plan.setApplicablePeriod(applicablePeriod);
            plan.setDefaultTimeSemantic(defaultTimeSemantic);
            plan.setSourceTablesJson(graphAssetSupport.writeJson(sourceTables, "[]"));
            plan.setNotes(notes);
            plan.setSqlText(sqlText);
            plan.setRetrievalText(buildPlanRetrievalText(scene, planName, applicablePeriod, sourceTables, notes));
            plan.setConfidenceScore(sqlText.isBlank() ? 0.55d : 0.85d);
            plan.setStatus(status);
            plan.setCreatedBy(operator);
            plan.setCreatedAt(now);
            plan.setUpdatedBy(operator);
            plan.setUpdatedAt(now);
            PlanPO savedPlan = planMapper.save(plan);

            CoverageDeclarationPO coverage = new CoverageDeclarationPO();
            coverage.setPlanId(savedPlan.getId());
            coverage.setCoverageCode(savedPlan.getPlanCode() + "-COV");
            coverage.setCoverageTitle(planName + "覆盖声明");
            coverage.setCoverageType("PERIOD_TABLE");
            coverage.setCoverageStatus(sourceTables.isEmpty() ? "PARTIAL" : "FULL");
            coverage.setStatementText(buildCoverageStatement(applicablePeriod, sourceTables));
            coverage.setApplicablePeriod(applicablePeriod);
            coverage.setTimeSemantic(Optional.ofNullable(savedPlan.getDefaultTimeSemantic()).filter(text -> !text.isBlank()).orElse("业务默认时间语义"));
            coverage.setSourceSystem(scene.getDomain());
            coverage.setSourceTablesJson(graphAssetSupport.writeJson(sourceTables, "[]"));
            coverage.setGapText(sourceTables.isEmpty() ? "来源表待补充" : "");
            coverage.setActive(true);
            coverage.setStatus(status);
            coverage.setCreatedBy(operator);
            coverage.setCreatedAt(now);
            coverage.setUpdatedBy(operator);
            coverage.setUpdatedAt(now);
            coverageDeclarationMapper.save(coverage);

            String fragmentText = sqlText.isBlank() ? Optional.ofNullable(scene.getRawInput()).orElse("") : sqlText;
            if (!fragmentText.isBlank()) {
                EvidenceFragmentPO evidence = new EvidenceFragmentPO();
                evidence.setSceneId(scene.getId());
                evidence.setEvidenceCode(savedPlan.getPlanCode() + "-EVD");
                evidence.setTitle(planName + "证据片段");
                evidence.setFragmentText(fragmentText);
                evidence.setSourceAnchor(scene.getSceneCode());
                evidence.setSourceType(sqlText.isBlank() ? "DOC_FRAGMENT" : "SQL_FRAGMENT");
                evidence.setSourceRef(scene.getSceneCode());
                evidence.setOriginType(evidence.getSourceType());
                evidence.setOriginRef(evidence.getSourceRef());
                evidence.setOriginLocator(evidence.getSourceAnchor());
                evidence.setConfidenceScore(savedPlan.getConfidenceScore());
                evidence.setStatus(status);
                evidence.setCreatedBy(operator);
                evidence.setCreatedAt(now);
                evidence.setUpdatedBy(operator);
                evidence.setUpdatedAt(now);
                EvidenceFragmentPO savedEvidence = evidenceFragmentMapper.save(evidence);

                PlanEvidenceRefPO ref = new PlanEvidenceRefPO();
                ref.setPlanId(savedPlan.getId());
                ref.setEvidenceId(savedEvidence.getId());
                ref.setRelationType("PRIMARY");
                ref.setCreatedBy(operator);
                ref.setCreatedAt(now);
                planEvidenceRefMapper.save(ref);
            }

            PlanPolicyRefPO policyRef = new PlanPolicyRefPO();
            policyRef.setPlanId(savedPlan.getId());
            policyRef.setPolicyId(defaultPolicy.getId());
            policyRef.setRelationType("ENFORCED");
            policyRef.setCreatedBy(operator);
            policyRef.setCreatedAt(now);
            planPolicyRefMapper.save(policyRef);

            createAlias(scene.getId(), savedPlan.getId(), savedPlan.getPlanCode(), planName, "PLAN", status, operator, now);
            for (String table : sourceTables) {
                createSchemaLink(savedPlan.getId(), table, null, "JOIN", status, operator, now);
            }
            for (String field : outputFields) {
                createSchemaLink(savedPlan.getId(), "SCENE_OUTPUT", field, "OUTPUT", status, operator, now);
            }
        }
    }

    private void ensureSceneLevelAliases(ScenePO scene, String status, String operator, OffsetDateTime now) {
        createAlias(scene.getId(), null, scene.getSceneCode() + "-SCENE", scene.getSceneTitle(), "SCENE", status, operator, now);
        if (scene.getDomain() != null && !scene.getDomain().isBlank()) {
            createAlias(scene.getId(), null, scene.getSceneCode() + "-DOMAIN", scene.getDomain(), "DOMAIN", status, operator, now);
        }
    }

    private PolicyPO ensureDefaultPolicy(ScenePO scene, String operator, OffsetDateTime now) {
        String semanticKey = scene.getSceneCode() + "::POLICY::DEFAULT_SCENE";
        PolicyPO existing = policyMapper.findAll().stream()
                .filter(policy -> "SCENE".equalsIgnoreCase(policy.getScopeType())
                        && scene.getId().equals(policy.getScopeRefId())
                        && policy.getPolicyCode() != null
                        && policy.getPolicyCode().equals(scene.getSceneCode() + "-PLC-DEFAULT"))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (existing.getPolicySemanticKey() == null || existing.getPolicySemanticKey().isBlank()) {
                existing.setPolicySemanticKey(semanticKey);
                existing.setUpdatedBy(operator);
                existing.setUpdatedAt(now);
                return policyMapper.save(existing);
            }
            return existing;
        }

        PolicyPO policy = new PolicyPO();
        policy.setPolicyCode(scene.getSceneCode() + "-PLC-DEFAULT");
        policy.setPolicyName(scene.getSceneTitle() + "默认访问策略");
        policy.setPolicySemanticKey(semanticKey);
        policy.setScopeType("SCENE");
        policy.setScopeRefId(scene.getId());
        policy.setEffectType("ALLOW");
        policy.setConditionText("默认允许已发布方案进入候选集；高敏字段由输出契约与人工审批补充约束。");
        policy.setSourceType("MIGRATED_INFERRED");
        policy.setSensitivityLevel("S1");
        policy.setMaskingRule("");
        policy.setStatus("PUBLISHED".equals(safeStatus(scene)) ? "ACTIVE" : "DRAFT");
        policy.setCreatedBy(operator);
        policy.setCreatedAt(now);
        policy.setUpdatedBy(operator);
        policy.setUpdatedAt(now);
        return policyMapper.save(policy);
    }

    private void deleteSceneScopedAssets(Long sceneId) {
        outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(outputContractMapper::delete);
        inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(inputSlotSchemaMapper::delete);
        sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(sourceIntakeContractMapper::delete);
        contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(contractViewMapper::delete);
        entityAliasMapper.deleteBySceneId(sceneId);

        List<PlanPO> existingPlans = planMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        for (PlanPO plan : existingPlans) {
            planEvidenceRefMapper.deleteByPlanId(plan.getId());
            planPolicyRefMapper.deleteByPlanId(plan.getId());
            planSchemaLinkMapper.deleteByPlanId(plan.getId());
            coverageDeclarationMapper.findByPlanIdOrderByUpdatedAtDesc(plan.getId()).forEach(coverageDeclarationMapper::delete);
            policyMapper.findAll().stream()
                    .filter(policy -> "PLAN".equalsIgnoreCase(policy.getScopeType()) && plan.getId().equals(policy.getScopeRefId()))
                    .forEach(policyMapper::delete);
            planMapper.delete(plan);
        }
        evidenceFragmentMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId).forEach(evidenceFragmentMapper::delete);
        policyMapper.findByFilter(null, sceneId, null).forEach(policyMapper::delete);
    }

    private void ensureDerivedPublishAssets(ScenePO scene,
                                            String status,
                                            String operator,
                                            OffsetDateTime now,
                                            boolean preferCreateViews,
                                            boolean preferCreateSourceContracts) {
        List<PlanPO> plans = planMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        if (plans.isEmpty()) {
            return;
        }

        List<InputSlotSchemaPO> inputSlots = inputSlotSchemaMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        List<OutputContractPO> outputContracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        OutputContractPO primaryOutput = outputContracts.stream()
                .max(Comparator.comparing(OutputContractPO::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(outputContracts.isEmpty() ? null : outputContracts.get(0));
        List<SourceIntakeContractPO> intakeContracts = sourceIntakeContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        SourceIntakeContractPO primaryIntake = intakeContracts.stream()
                .max(Comparator.comparing(SourceIntakeContractPO::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(intakeContracts.isEmpty() ? null : intakeContracts.get(0));

        String sceneTimeSemantic = inferDefaultTimeSemantic(
                scene,
                null,
                graphAssetSupport.parseStringList(scene.getSourceTablesJson()),
                String.join(" ", List.of(
                        Optional.ofNullable(scene.getSceneTitle()).orElse(""),
                        Optional.ofNullable(scene.getSceneDescription()).orElse(""),
                        Optional.ofNullable(scene.getRawInput()).orElse("")
                ))
        );

        if (primaryIntake != null) {
            if (primaryIntake.getDefaultTimeSemantic() == null || primaryIntake.getDefaultTimeSemantic().isBlank()) {
                primaryIntake.setDefaultTimeSemantic(sceneTimeSemantic);
            }
            if (primaryIntake.getSourceTableHintsJson() == null || primaryIntake.getSourceTableHintsJson().isBlank()) {
                primaryIntake.setSourceTableHintsJson(graphAssetSupport.writeJson(graphAssetSupport.parseStringList(scene.getSourceTablesJson()), "[]"));
            }
            if (primaryIntake.getKnownCoverageJson() == null || primaryIntake.getKnownCoverageJson().isBlank()) {
                ArrayNode knownCoverage = graphAssetSupport.parseArray("[]");
                plans.stream()
                        .map(PlanPO::getApplicablePeriod)
                        .filter(text -> text != null && !text.isBlank())
                        .distinct()
                        .forEach(knownCoverage::add);
                primaryIntake.setKnownCoverageJson(graphAssetSupport.writeJson(knownCoverage, "[]"));
            }
            primaryIntake.setUpdatedBy(operator);
            primaryIntake.setUpdatedAt(now);
            sourceIntakeContractMapper.save(primaryIntake);
        }

        int nextViewIndex = contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId()).size();
        List<ContractViewPO> existingViews = contractViewMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        String primaryIdentifierType = derivePrimaryIdentifierType(inputSlots);

        for (PlanPO plan : plans) {
            if (plan.getDefaultTimeSemantic() == null || plan.getDefaultTimeSemantic().isBlank()) {
                List<String> sourceTables = graphAssetSupport.parseStringList(plan.getSourceTablesJson());
                if (sourceTables.isEmpty()) {
                    sourceTables = inferSourceTablesFromText(plan.getSqlText());
                    if (!sourceTables.isEmpty()) {
                        plan.setSourceTablesJson(graphAssetSupport.writeJson(sourceTables, "[]"));
                    }
                }
                plan.setDefaultTimeSemantic(inferDefaultTimeSemantic(scene, plan.getPlanName(), sourceTables, plan.getSqlText()));
                plan.setUpdatedBy(operator);
                plan.setUpdatedAt(now);
                planMapper.save(plan);
            }

            boolean hasPlanView = existingViews.stream().anyMatch(view -> plan.getId().equals(view.getPlanId()));
            if (preferCreateViews && !hasPlanView && primaryOutput != null) {
                nextViewIndex += 1;
                ContractViewPO view = new ContractViewPO();
                view.setSceneId(scene.getId());
                view.setPlanId(plan.getId());
                view.setOutputContractId(primaryOutput.getId());
                view.setViewCode(scene.getSceneCode() + "-VIEW-" + String.format(Locale.ROOT, "%02d", nextViewIndex));
                view.setViewName(plan.getPlanName() + "默认契约视图");
                view.setRoleScope("DEFAULT_ROLE");
                view.setVisibleFieldsJson(buildContractViewVisibleFields(primaryOutput));
                view.setMaskedFieldsJson("[]");
                view.setRestrictedFieldsJson("[]");
                view.setForbiddenFieldsJson("[]");
                view.setApprovalTemplate("");
                view.setStatus(status);
                view.setCreatedBy(operator);
                view.setCreatedAt(now);
                view.setUpdatedBy(operator);
                view.setUpdatedAt(now);
                contractViewMapper.save(view);
                existingViews.add(view);
            }

            List<String> sourceTables = graphAssetSupport.parseStringList(plan.getSourceTablesJson());
            if (sourceTables.isEmpty()) {
                sourceTables = inferSourceTablesFromText(plan.getSqlText());
                if (!sourceTables.isEmpty()) {
                    plan.setSourceTablesJson(graphAssetSupport.writeJson(sourceTables, "[]"));
                    plan.setUpdatedBy(operator);
                    plan.setUpdatedAt(now);
                    planMapper.save(plan);
                }
            }
        }

        if (preferCreateSourceContracts) {
            syncSourceContractsIncrementally(
                    scene,
                    plans,
                    primaryIntake,
                    status,
                    operator,
                    now,
                    primaryIdentifierType,
                    sceneTimeSemantic
            );
        }
    }

    private void syncSourceContractsIncrementally(ScenePO scene,
                                                  List<PlanPO> plans,
                                                  SourceIntakeContractPO primaryIntake,
                                                  String status,
                                                  String operator,
                                                  OffsetDateTime now,
                                                  String primaryIdentifierType,
                                                  String sceneTimeSemantic) {
        List<SourceContractPO> existingContracts = sourceContractMapper.findBySceneIdOrderByUpdatedAtDesc(scene.getId());
        Map<String, SourceContractPO> latestByNormalizedTable = new LinkedHashMap<>();
        for (SourceContractPO contract : existingContracts) {
            String normalizedTable = normalizePhysicalTable(
                    Optional.ofNullable(contract.getNormalizedPhysicalTable()).filter(text -> !text.isBlank()).orElse(contract.getPhysicalTable())
            );
            if (!normalizedTable.isBlank()) {
                latestByNormalizedTable.putIfAbsent(normalizedTable, contract);
            }
        }

        int nextSourceContractIndex = existingContracts.size();
        LinkedHashSet<String> desiredNormalizedTables = new LinkedHashSet<>();
        for (PlanPO plan : plans) {
            List<String> sourceTables = graphAssetSupport.parseStringList(plan.getSourceTablesJson());
            for (String sourceTable : sourceTables) {
                String normalizedTable = normalizePhysicalTable(sourceTable);
                if (normalizedTable.isBlank()) {
                    continue;
                }
                desiredNormalizedTables.add(normalizedTable);

                SourceContractPO contract = latestByNormalizedTable.get(normalizedTable);
                if (contract == null) {
                    nextSourceContractIndex += 1;
                    contract = new SourceContractPO();
                    contract.setSceneId(scene.getId());
                    contract.setSourceContractCode(scene.getSceneCode() + "-SRC-CON-" + String.format(Locale.ROOT, "%02d", nextSourceContractIndex));
                    contract.setCreatedBy(operator);
                    contract.setCreatedAt(now);
                    latestByNormalizedTable.put(normalizedTable, contract);
                }

                contract.setPlanId(plan.getId());
                contract.setIntakeContractId(primaryIntake == null ? null : primaryIntake.getId());
                contract.setSourceName(sourceTable.trim() + "来源契约");
                contract.setPhysicalTable(sourceTable.trim());
                contract.setNormalizedPhysicalTable(normalizedTable);
                contract.setSourceRole("PRIMARY_QUERY");
                contract.setIdentifierType(primaryIdentifierType);
                contract.setOutputIdentifierType(primaryIdentifierType);
                contract.setSourceSystem(scene.getDomain());
                contract.setTimeSemantic(Optional.ofNullable(plan.getDefaultTimeSemantic()).filter(text -> !text.isBlank()).orElse(sceneTimeSemantic));
                contract.setCompletenessLevel(sourceTables.size() > 1 ? "PARTIAL" : "FULL");
                contract.setSensitivityLevel("S1");
                contract.setMaterialSourceNote(primaryIntake == null ? "由场景历史资料自动推断生成" : Optional.ofNullable(primaryIntake.getMaterialSourceNote()).orElse("由场景历史资料自动推断生成"));
                contract.setNotes(sourceTables.size() > 1 ? "当前方案命中多来源表，请结合覆盖声明确认回退策略" : "由场景导入结果自动派生");
                contract.setStatus(status);
                contract.setUpdatedBy(operator);
                contract.setUpdatedAt(now);
                sourceContractMapper.save(contract);
            }
        }

        for (SourceContractPO contract : existingContracts) {
            String normalizedTable = normalizePhysicalTable(
                    Optional.ofNullable(contract.getNormalizedPhysicalTable()).filter(text -> !text.isBlank()).orElse(contract.getPhysicalTable())
            );
            SourceContractPO latest = normalizedTable.isBlank() ? null : latestByNormalizedTable.get(normalizedTable);
            boolean staleDuplicate = latest != null && latest.getId() != null && !latest.getId().equals(contract.getId());
            boolean missingFromScene = normalizedTable.isBlank() || !desiredNormalizedTables.contains(normalizedTable);
            if ((staleDuplicate || missingFromScene) && !"DEPRECATED".equalsIgnoreCase(contract.getStatus())) {
                contract.setStatus("DEPRECATED");
                contract.setUpdatedBy(operator);
                contract.setUpdatedAt(now);
                sourceContractMapper.save(contract);
            }
        }
    }

    private void createAlias(Long sceneId, Long planId, String aliasCode, String aliasText, String aliasType,
                             String status, String operator, OffsetDateTime now) {
        if (aliasText == null || aliasText.isBlank()) {
            return;
        }
        EntityAliasPO alias = entityAliasMapper.findByAliasCode(aliasCode).orElseGet(EntityAliasPO::new);
        if (alias.getId() == null) {
            alias.setAliasCode(aliasCode);
            alias.setCreatedBy(operator);
            alias.setCreatedAt(now);
        }
        alias.setSceneId(sceneId);
        alias.setPlanId(planId);
        alias.setAliasText(aliasText.trim());
        alias.setNormalizedText(graphAssetSupport.normalizeAlias(aliasText));
        alias.setAliasType(aliasType);
        alias.setSource("SCENE_SYNC");
        alias.setConfidenceScore(0.9d);
        alias.setStatus(status);
        alias.setUpdatedBy(operator);
        alias.setUpdatedAt(now);
        entityAliasMapper.save(alias);
    }

    private void createSchemaLink(Long planId, String tableName, String columnName, String role,
                                  String status, String operator, OffsetDateTime now) {
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        PlanSchemaLinkPO link = new PlanSchemaLinkPO();
        link.setPlanId(planId);
        link.setTableName(tableName.trim());
        link.setColumnName(columnName == null || columnName.isBlank() ? null : columnName.trim());
        link.setLinkRole(role);
        link.setEvidenceId(null);
        link.setConfidenceScore(columnName == null ? 0.72d : 0.88d);
        link.setStatus(status);
        link.setCreatedBy(operator);
        link.setCreatedAt(now);
        link.setUpdatedBy(operator);
        link.setUpdatedAt(now);
        planSchemaLinkMapper.save(link);
    }

    private void inferPlaceholders(String text, LinkedHashSet<String> collector) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            collector.add(matcher.group(1));
        }
    }

    private String readOutputSummary(String outputsJson) {
        JsonNode node = graphAssetSupport.parseJson(outputsJson, "{}");
        if (node.isArray()) {
            return "";
        }
        return graphAssetSupport.safeText(node.path("summary"));
    }

    private String readOutputFieldsJson(String outputsJson) {
        JsonNode node = graphAssetSupport.parseJson(outputsJson, "{}");
        if (node.isArray()) {
            return graphAssetSupport.writeJson(node, "[]");
        }
        JsonNode fields = node.path("fields");
        if (fields.isArray()) {
            return graphAssetSupport.writeJson(fields, "[]");
        }
        return "[]";
    }

    private List<String> readOutputFields(Long sceneId) {
        List<OutputContractPO> contracts = outputContractMapper.findBySceneIdOrderByUpdatedAtDesc(sceneId);
        if (!contracts.isEmpty()) {
            JsonNode fields = graphAssetSupport.parseJson(contracts.get(0).getFieldsJson(), "[]");
            List<String> names = new ArrayList<>();
            if (fields.isArray()) {
                fields.forEach(field -> {
                    String text = graphAssetSupport.safeText(field.path("name"));
                    if (text.isBlank()) {
                        text = graphAssetSupport.safeText(field.path("field_name"));
                    }
                    if (!text.isBlank()) {
                        names.add(text);
                    }
                });
            }
            return names;
        }
        JsonNode root = graphAssetSupport.parseJson(sceneMapper.findById(sceneId).map(ScenePO::getOutputsJson).orElse(null), "{}");
        JsonNode fields = root.path("fields");
        List<String> names = new ArrayList<>();
        if (fields.isArray()) {
            fields.forEach(field -> {
                String text = graphAssetSupport.safeText(field.path("name"));
                if (!text.isBlank()) {
                    names.add(text);
                }
            });
        }
        return names;
    }

    private List<String> readSourceTables(JsonNode node, String fallbackJson) {
        if (node != null && node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    values.add(text);
                }
            });
            if (!values.isEmpty()) {
                return values;
            }
        }
        return graphAssetSupport.parseStringList(fallbackJson);
    }

    private String readSqlText(JsonNode variant) {
        String sqlText = graphAssetSupport.safeText(variant.path("sql_text"));
        if (!sqlText.isBlank()) {
            return sqlText;
        }
        sqlText = graphAssetSupport.safeText(variant.path("sql"));
        if (!sqlText.isBlank()) {
            return sqlText;
        }
        return graphAssetSupport.safeText(variant.path("sql_block"));
    }

    private List<String> inferSourceTablesFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?i)\\b(?:from|join)\\s+([A-Za-z0-9_$.]+)").matcher(text);
        while (matcher.find()) {
            String table = matcher.group(1) == null ? "" : matcher.group(1).trim();
            if (table.isBlank()) {
                continue;
            }
            table = table.replaceAll("[,;)]$", "");
            if (!table.isBlank()) {
                tables.add(table);
            }
        }
        return new ArrayList<>(tables);
    }

    private String derivePrimaryIdentifierType(List<InputSlotSchemaPO> inputSlots) {
        if (inputSlots == null || inputSlots.isEmpty()) {
            return "PROTOCOL_NBR";
        }
        for (InputSlotSchemaPO slot : inputSlots) {
            for (String candidate : graphAssetSupport.parseStringList(slot.getIdentifierCandidatesJson())) {
                String normalized = candidate == null ? "" : candidate.trim();
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
            String slotName = slot.getSlotName() == null ? "" : slot.getSlotName().trim();
            if (!slotName.isBlank()) {
                return slotName.toUpperCase(Locale.ROOT).replace(' ', '_');
            }
        }
        return "PROTOCOL_NBR";
    }

    private String buildContractViewVisibleFields(OutputContractPO contract) {
        if (contract == null) {
            return "[]";
        }
        JsonNode node = graphAssetSupport.parseJson(contract.getFieldsJson(), "[]");
        ArrayNode visibleFields = graphAssetSupport.parseArray("[]");
        if (node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual()) {
                    String value = item.asText("").trim();
                    if (!value.isBlank()) {
                        visibleFields.add(value);
                    }
                    return;
                }
                if (item.isObject()) {
                    for (String key : List.of("fieldName", "name", "fieldCode", "code", "columnName")) {
                        String value = graphAssetSupport.safeText(item.path(key));
                        if (!value.isBlank()) {
                            visibleFields.add(value);
                            break;
                        }
                    }
                }
            });
        }
        return graphAssetSupport.writeJson(visibleFields, "[]");
    }

    private String inferDefaultTimeSemantic(ScenePO scene, String planName, Iterable<String> sourceTables, String sqlText) {
        StringBuilder builder = new StringBuilder();
        builder.append(Optional.ofNullable(scene.getSceneTitle()).orElse("")).append(' ')
                .append(Optional.ofNullable(scene.getSceneDescription()).orElse("")).append(' ')
                .append(Optional.ofNullable(planName).orElse("")).append(' ')
                .append(Optional.ofNullable(sqlText).orElse("")).append(' ')
                .append(Optional.ofNullable(scene.getRawInput()).orElse(""));
        if (sourceTables != null) {
            for (String table : sourceTables) {
                builder.append(' ').append(Optional.ofNullable(table).orElse(""));
            }
        }
        String corpus = builder.toString().toUpperCase(Locale.ROOT);
        if (corpus.contains("TRX_DT") || corpus.contains("TRX_TIME") || corpus.contains("交易日期") || corpus.contains("交易时间")) {
            return "交易日期";
        }
        if (corpus.contains("ACCOUNTING_DATE") || corpus.contains("POSTING_DATE") || corpus.contains("记账日期")) {
            return "记账日期";
        }
        if (corpus.contains("OPEN_DATE") || corpus.contains("开户日期")) {
            return "开户日期";
        }
        if (corpus.contains("MONTH") || corpus.contains("月份") || corpus.contains("月度")) {
            return "统计月份";
        }
        if (corpus.contains("DATE_FROM") || corpus.contains("DATE_TO") || corpus.contains("TIME_FROM") || corpus.contains("TIME_TO")) {
            return "业务发生日期";
        }
        return "业务发生日期";
    }

    private String buildPlanRetrievalText(ScenePO scene, String planName, String applicablePeriod, List<String> sourceTables, String notes) {
        return String.join(" ", List.of(
                Optional.ofNullable(scene.getSceneTitle()).orElse(""),
                Optional.ofNullable(scene.getSceneDescription()).orElse(""),
                planName,
                Optional.ofNullable(applicablePeriod).orElse(""),
                String.join(" ", sourceTables),
                Optional.ofNullable(notes).orElse(""),
                Optional.ofNullable(scene.getSceneType()).orElse("FACT_DETAIL")
        )).trim();
    }

    private String buildCoverageStatement(String applicablePeriod, List<String> sourceTables) {
        String period = applicablePeriod == null || applicablePeriod.isBlank() ? "未显式声明时段" : applicablePeriod;
        String tables = sourceTables.isEmpty() ? "来源表待补充" : String.join("、", sourceTables);
        return "覆盖时段：" + period + "；主要来源：" + tables;
    }

    private String safeStatus(ScenePO scene) {
        return safeAssetStatus(scene == null || scene.getStatus() == null ? null : scene.getStatus().name());
    }

    private String safeAssetStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DRAFT";
        }
        return graphAssetSupport.normalizeStatus(status, "DRAFT");
    }

    private String normalizePhysicalTable(String sourceTable) {
        if (sourceTable == null || sourceTable.isBlank()) {
            return "";
        }
        return sourceTable.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private boolean appliesToScene(Long sceneId, PolicyPO policy) {
        if (policy == null) {
            return false;
        }
        if ("SCENE".equalsIgnoreCase(policy.getScopeType())) {
            return sceneId.equals(policy.getScopeRefId());
        }
        if ("PLAN".equalsIgnoreCase(policy.getScopeType()) && policy.getScopeRefId() != null) {
            return planMapper.findById(policy.getScopeRefId())
                    .map(plan -> sceneId.equals(plan.getSceneId()))
                    .orElse(false);
        }
        return false;
    }
}
