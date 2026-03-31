package com.cmbchina.datadirect.caliber.adapter.web;

import static org.hamcrest.Matchers.hasItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocumentWithCorePaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/system/auth/token'].post.tags", hasItem("系统鉴权")))
                .andExpect(jsonPath("$.paths['/api/system/auth/token'].post.summary").value("获取访问令牌"))
                .andExpect(jsonPath("$.paths['/api/system/auth/token'].post.operationId").value("issueAuthToken"))
                .andExpect(jsonPath("$.paths['/api/system/auth/token'].post.responses['200'].content.*.schema.$ref").value(hasItem("#/components/schemas/AuthTokenDTO")))
                .andExpect(jsonPath("$.paths['/api/system/auth/token'].post.responses['500'].content.*.schema.$ref").value(hasItem("#/components/schemas/ApiErrorDTO")))

                .andExpect(jsonPath("$.paths['/api/domains'].get.tags", hasItem("业务领域")))
                .andExpect(jsonPath("$.paths['/api/domains'].get.summary").value("查询业务领域列表"))
                .andExpect(jsonPath("$.paths['/api/domains'].get.operationId").value("listDomains"))

                .andExpect(jsonPath("$.paths['/api/domains/{id}'].get.tags", hasItem("业务领域")))
                .andExpect(jsonPath("$.paths['/api/domains/{id}'].get.summary").value("查询业务领域详情"))
                .andExpect(jsonPath("$.paths['/api/domains/{id}'].get.operationId").value("getDomainById"))

                .andExpect(jsonPath("$.paths['/api/domains'].post.tags", hasItem("业务领域")))
                .andExpect(jsonPath("$.paths['/api/domains'].post.summary").value("创建业务领域"))
                .andExpect(jsonPath("$.paths['/api/domains'].post.operationId").value("createDomain"))
                .andExpect(jsonPath("$.paths['/api/domains'].post.responses['201'].content.*.schema.$ref").value(hasItem("#/components/schemas/DomainDTO")))
                .andExpect(jsonPath("$.paths['/api/domains'].post.responses['400'].content.*.schema.$ref").value(hasItem("#/components/schemas/ApiErrorDTO")))

                .andExpect(jsonPath("$.paths['/api/scenes'].get.tags", hasItem("场景设计")))
                .andExpect(jsonPath("$.paths['/api/scenes'].get.summary").value("查询场景列表"))
                .andExpect(jsonPath("$.paths['/api/scenes'].get.operationId").value("listScenes"))

                .andExpect(jsonPath("$.paths['/api/scenes'].post.tags", hasItem("场景设计")))
                .andExpect(jsonPath("$.paths['/api/scenes'].post.summary").value("创建场景"))
                .andExpect(jsonPath("$.paths['/api/scenes'].post.operationId").value("createScene"))
                .andExpect(jsonPath("$.paths['/api/scenes'].post.responses['201'].content.*.schema.$ref").value(hasItem("#/components/schemas/SceneDTO")))
                .andExpect(jsonPath("$.paths['/api/scenes'].post.responses['400'].content.*.schema.$ref").value(hasItem("#/components/schemas/ApiErrorDTO")))

                .andExpect(jsonPath("$.paths['/api/graphrag/query'].post.tags", hasItem("图谱检索与知识包")))
                .andExpect(jsonPath("$.paths['/api/graphrag/query'].post.summary").value("检索知识包"))
                .andExpect(jsonPath("$.paths['/api/graphrag/query'].post.operationId").value("queryKnowledgePackage"))

                .andExpect(jsonPath("$.paths['/api/scene-search'].post.tags", hasItem("图谱检索与知识包")))
                .andExpect(jsonPath("$.paths['/api/scene-search'].post.summary").value("场景检索"))
                .andExpect(jsonPath("$.paths['/api/scene-search'].post.operationId").value("sceneSearch"))

                .andExpect(jsonPath("$.paths['/api/plan-select'].post.tags", hasItem("图谱检索与知识包")))
                .andExpect(jsonPath("$.paths['/api/plan-select'].post.summary").value("方案选择"))
                .andExpect(jsonPath("$.paths['/api/plan-select'].post.operationId").value("planSelect"))

                .andExpect(jsonPath("$.paths['/api/graphrag/projection/{sceneId}'].get.tags", hasItem("图谱检索与知识包")))
                .andExpect(jsonPath("$.paths['/api/graphrag/projection/{sceneId}'].get.summary").value("查询投影状态"))
                .andExpect(jsonPath("$.paths['/api/graphrag/projection/{sceneId}'].get.operationId").value("queryProjectionStatus"))

                .andExpect(jsonPath("$.paths['/api/graphrag/rebuild/{sceneId}'].post.tags", hasItem("图谱检索与知识包")))
                .andExpect(jsonPath("$.paths['/api/graphrag/rebuild/{sceneId}'].post.summary").value("重建投影"))
                .andExpect(jsonPath("$.paths['/api/graphrag/rebuild/{sceneId}'].post.operationId").value("rebuildProjection"))

                .andExpect(jsonPath("$.paths['/api/datamap/graph'].get.tags", hasItem("数据地图")))
                .andExpect(jsonPath("$.paths['/api/datamap/graph'].get.summary").value("查询数据地图"))
                .andExpect(jsonPath("$.paths['/api/datamap/graph'].get.operationId").value("queryDatamapGraph"))

                .andExpect(jsonPath("$.paths['/api/datamap/node/{id}/detail'].get.tags", hasItem("数据地图")))
                .andExpect(jsonPath("$.paths['/api/datamap/node/{id}/detail'].get.summary").value("查询数据地图节点详情"))
                .andExpect(jsonPath("$.paths['/api/datamap/node/{id}/detail'].get.operationId").value("queryDatamapNodeDetail"))

                .andExpect(jsonPath("$.paths['/api/datamap/impact-analysis'].post.tags", hasItem("数据地图")))
                .andExpect(jsonPath("$.paths['/api/datamap/impact-analysis'].post.summary").value("分析影响范围"))
                .andExpect(jsonPath("$.paths['/api/datamap/impact-analysis'].post.operationId").value("analyzeDatamapImpact"))

                .andExpect(jsonPath("$.components.schemas['ApiErrorDTO'].description").exists())
                .andExpect(jsonPath("$.components.schemas['SceneDTO'].description").exists())
                .andExpect(jsonPath("$.components.schemas['DataMapGraphResponseDTO'].description").exists())
                .andExpect(jsonPath("$.paths['/api/scenes']").exists())
                .andExpect(jsonPath("$.paths['/api/graphrag/query']").exists())
                .andExpect(jsonPath("$.paths['/api/import/preprocess-stream']").exists())
                .andExpect(jsonPath("$.components.schemas.PreprocessResultDTO.properties.candidateGraph").exists());
    }
}
