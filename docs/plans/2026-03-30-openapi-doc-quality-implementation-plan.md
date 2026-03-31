# OpenAPI Doc Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the formal API contract quality gate for `09b-unified-api-and-event-contracts.md` into executable form so `/v3/api-docs` exposes stable business tags, summaries, operation IDs, and error models.

**Architecture:** Keep SpringDoc as the single contract generator. Lock the target shape with an integration test, then annotate controllers and shared response models until business-domain tags, stable `operationId`, and public error contracts are machine-verifiable and human-readable.

**Tech Stack:** Spring Boot, SpringDoc OpenAPI, JUnit 5, MockMvc-style integration tests

---

## Design Inputs

- `docs/architecture/features/iteration-02-runtime-and-governance/09b-unified-api-and-event-contracts.md`
- `docs/engineering/current-delivery-status.md`
- `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`

## File Map

- Create: `docs/testing/features/iteration-02-runtime-and-governance/openapi-doc-quality-test-report.md`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/OpenApiConfig.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DomainController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphRagController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ApiErrorDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GlobalExceptionHandler.java`

### Task 1: Create the OpenAPI quality gate report

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/openapi-doc-quality-test-report.md`

- [ ] Add the target gate items for business tags, summary, operation ID, and common error model coverage.
- [ ] Record the exact contract assertions that implementation must satisfy.

Run: `test -f docs/testing/features/iteration-02-runtime-and-governance/openapi-doc-quality-test-report.md`

Expected: exit code `0`

### Task 2: Add failing integration assertions for the contract shape

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`

- [ ] Assert that core endpoints are grouped by business tags rather than `*-controller`.
- [ ] Assert that representative operations expose stable `summary`, `operationId`, and error response schemas.

Run: `cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`

Expected: FAIL because the current `/v3/api-docs` still contains auto-derived tags, unstable operation IDs, or missing error-model docs.

### Task 3: Add global OpenAPI metadata and shared error schema annotations

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/OpenApiConfig.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ApiErrorDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GlobalExceptionHandler.java`

- [ ] Add top-level OpenAPI metadata and reusable error response registration.
- [ ] Make the common error DTO self-describing in generated schema output.

Run: `cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`

Expected: Some assertions remain red, but shared-model and response-schema failures are reduced.

### Task 4: Annotate core controllers with stable business tags and operation IDs

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DomainController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/SceneController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ImportController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DataMapGraphController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/GraphRagController.java`

- [ ] Add controller-level business tags.
- [ ] Add endpoint-level summaries, descriptions where needed, and explicit stable `operationId`.
- [ ] Expose expected success and error responses for the highest-frequency endpoints.

Run: `cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`

Expected: PASS.

### Task 5: Verify live docs output and update the report

**Files:**

- Modify: `docs/testing/features/iteration-02-runtime-and-governance/openapi-doc-quality-test-report.md`

- [ ] Re-run the integration test and live docs smoke check.
- [ ] Record pass evidence and any deferred model-description backlog.

Run:

- `cd backend && mvn -q -Dtest=OpenApiDocumentationIntegrationTest test`
- `bash scripts/start_backend.sh`
- `curl -sSf http://127.0.0.1:8082/v3/api-docs | jq -r '.openapi, (.tags | length), (.paths | length)'`

Expected:

- Test PASS.
- Live docs return OpenAPI payload with non-zero `tags` and `paths`.
