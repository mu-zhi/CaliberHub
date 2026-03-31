package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapGraphResponseDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.DataMapNodeDetailDTO;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ProjectionVerificationStatus;
import com.cmbchina.datadirect.caliber.application.api.dto.response.datamap.ReadSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraphQueryService {

    private static final Logger log = LoggerFactory.getLogger(GraphQueryService.class);

    private final GraphReadService graphReadService;
    private final DataMapGraphDtoAdapter dataMapGraphDtoAdapter;
    private final ReadSourceRouter readSourceRouter;
    private final Neo4jGraphReadService neo4jGraphReadService;

    public GraphQueryService(GraphReadService graphReadService,
                             DataMapGraphDtoAdapter dataMapGraphDtoAdapter,
                             ReadSourceRouter readSourceRouter,
                             Neo4jGraphReadService neo4jGraphReadService) {
        this.graphReadService = graphReadService;
        this.dataMapGraphDtoAdapter = dataMapGraphDtoAdapter;
        this.readSourceRouter = readSourceRouter;
        this.neo4jGraphReadService = neo4jGraphReadService;
    }

    @Transactional(readOnly = true)
    public DataMapGraphResponseDTO queryGraph(String rootType,
                                              Long rootId,
                                              Long snapshotId,
                                              String objectTypes,
                                              String statuses,
                                              String relationTypes,
                                              String sensitivityScopes) {
        Long sceneId = graphReadService.resolveSceneId(rootType, rootId);
        ReadSourceRouter.ReadSourceDecision decision = readSourceRouter.decide(sceneId, snapshotId);
        DataMapGraphQueryOptions options = DataMapGraphQueryOptions.of(snapshotId, objectTypes, statuses, relationTypes, sensitivityScopes);

        if (decision.readSource() == ReadSource.NEO4J) {
            try {
                Neo4jGraphResult neo4jResult = neo4jGraphReadService.readGraph(sceneId, decision.snapshotId(), options);
                return new DataMapGraphResponseDTO(
                        neo4jResult.rootRef(), neo4jResult.sceneId(), neo4jResult.sceneName(),
                        decision.snapshotId(), ReadSource.NEO4J,
                        decision.verificationStatus(), decision.verifiedAt(),
                        neo4jResult.nodes(), neo4jResult.edges());
            } catch (Exception ex) {
                log.warn("Neo4j read failed for scene={}, falling back to relational: {}",
                        sceneId, ex.getMessage());
                return queryFromRelational(rootType, rootId, sceneId, decision, options);
            }
        }

        return queryFromRelational(rootType, rootId, sceneId, decision, options);
    }

    private DataMapGraphResponseDTO queryFromRelational(String rootType, Long rootId, Long sceneId,
                                                         ReadSourceRouter.ReadSourceDecision decision,
                                                         DataMapGraphQueryOptions options) {
        GraphSceneBundle bundle = graphReadService.loadBundle(rootType, rootId);
        String rootRef = normalizeRootRef(rootType, rootId);
        DataMapGraphResponseDTO relationalResult = dataMapGraphDtoAdapter.buildGraph(rootRef, bundle, options);
        return new DataMapGraphResponseDTO(
                relationalResult.rootRef(), relationalResult.sceneId(), relationalResult.sceneName(),
                decision.snapshotId(), ReadSource.RELATIONAL,
                decision.verificationStatus(), decision.verifiedAt(),
                relationalResult.nodes(), relationalResult.edges());
    }

    @Transactional(readOnly = true)
    public DataMapNodeDetailDTO queryNodeDetail(String assetRef) {
        GraphSceneBundle bundle = graphReadService.loadBundleByAssetRef(assetRef);
        return dataMapGraphDtoAdapter.buildNodeDetail(assetRef, bundle);
    }

    private String normalizeRootRef(String rootType, Long rootId) {
        ResolvedAssetRef resolved = new ResolvedAssetRef(
                rootType == null ? "" : rootType.trim().replace('-', '_').replace(' ', '_').toUpperCase(),
                rootId
        );
        return switch (resolved.objectType()) {
            case "SCENE" -> "scene:" + resolved.numericId();
            case "PLAN" -> "plan:" + resolved.numericId();
            case "OUTPUT_CONTRACT" -> "output-contract:" + resolved.numericId();
            case "CONTRACT_VIEW" -> "contract-view:" + resolved.numericId();
            case "COVERAGE_DECLARATION" -> "coverage-declaration:" + resolved.numericId();
            case "POLICY" -> "policy:" + resolved.numericId();
            case "EVIDENCE_FRAGMENT" -> "evidence-fragment:" + resolved.numericId();
            case "SOURCE_CONTRACT" -> "source-contract:" + resolved.numericId();
            case "SOURCE_INTAKE_CONTRACT" -> "source-intake-contract:" + resolved.numericId();
            case "PATH_TEMPLATE" -> "path-template:" + resolved.numericId();
            case "DOMAIN" -> "domain:" + resolved.numericId();
            case "VERSION_SNAPSHOT" -> "version-snapshot:" + resolved.numericId();
            default -> "scene:" + resolved.numericId();
        };
    }
}
