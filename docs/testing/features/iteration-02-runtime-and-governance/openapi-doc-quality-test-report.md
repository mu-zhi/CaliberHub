# OpenAPI 文档质量治理测试报告

> 对应实施计划：`docs/plans/2026-03-30-openapi-doc-quality-implementation-plan.md`
> 日期：`2026-03-31`

## 1. 任务目标

- 收敛关键正式接口 OpenAPI 元数据到业务域分组（Tag）。
- 固化 `summary` 与稳定 `operationId`。
- 补齐统一成功响应与 `ApiErrorDTO` 错误响应可回归验证。
- 补齐核心模型 `ApiErrorDTO / SceneDTO / DataMapGraphResponseDTO` 的 `description`。
- 通过 `OpenApiDocumentationIntegrationTest` 以及 `/v3/api-docs` 运行时探活。

## 2. 测试入口与环境

- 单元/集成测试入口：
  - `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- 测试命令（建议）：
  - `cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`
- 服务探活：
  - `bash scripts/start_backend.sh`
  - `curl -sSf http://127.0.0.1:8082/v3/api-docs`

## 3. 结果记录

### 3.1 集成测试

- 3.1.1 OpenAPI 契约断言（任务范围关键路径）
  - 状态：通过
  - 预期：关键路径 `tag / summary / operationId / response / schema` 全部通过
  - 实际：`backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
    通过命令：`cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`
  - 关联路径：
    - `/api/system/auth/token`
    - `/api/domains*`
    - `/api/scenes*`
    - `/api/graphrag/query`
    - `/api/scene-search`
    - `/api/plan-select`
    - `/api/graphrag/projection/{sceneId}`
    - `/api/graphrag/rebuild/{sceneId}`
    - `/api/datamap/graph`
    - `/api/datamap/node/{id}/detail`
    - `/api/datamap/impact-analysis`

### 3.2 运行时探活

- 3.2.1 `GET /v3/api-docs`
  - 状态：通过
  - 期望：HTTP 200；关键契约元数据可见
  - 实际：`curl -sSf http://127.0.0.1:8082/v3/api-docs` 返回 `200`；
    关键路径与标签均存在，`/api/system/auth/token` 的 `operationId` 为 `issueAuthToken`。
    注：本次探活会话未使用 `scripts/start_backend.sh`（脚本在当前环境会报 `ENV_FILE` 兼容问题），改为执行 `mvn spring-boot:run` 后完成。

## 4. 变更清单（本轮）

- 修改文件
  - `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/AuthController.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DomainController.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneController.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphRagController.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ApiErrorDTO.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/SceneDTO.java`
  - `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/datamap/DataMapGraphResponseDTO.java`

## 5. 开放项

- 继续补齐其他控制器与模型的 `summary / description / response` 注解，形成下一轮的默认质量门禁扩展。
