# Real Database Control Asset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the runtime `H2（内存数据库）` baseline with a `MySQL（关系型数据库）` compatible datasource configuration and persist `Source Material（来源材料）` as an independent control asset linked to `Import Task（导入任务）`.

**Architecture:** Keep the existing `Spring Boot（后端框架） + Flyway（数据库迁移工具） + JPA（对象关系映射）` stack, but move import persistence to a formal two-layer model: `Source Material` holds the uploaded content and metadata, while `Import Task` remains a process-state record referencing `material_id`. Runtime defaults switch to environment-driven MySQL-compatible values, while test profile stays isolated for red-green verification.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, MySQL Connector/J, H2 test profile, MockMvc, JUnit 5

---

### Task 1: Add failing integration coverage for material persistence and runtime datasource defaults

**Files:**

- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskApiIntegrationTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/infrastructure/common/config/DataSourceRuntimeConfigTest.java`

- [ ] **Step 1: Write the failing import-material test**

Extend `ImportTaskApiIntegrationTest` so preprocess response and task detail both require `materialId`, and the persisted material row can be loaded from a repository:

```java
@Autowired
private SourceMaterialMapper sourceMaterialMapper;

@Test
void shouldPersistIndependentSourceMaterialForImportTask() throws Exception {
    String token = loginAndGetToken("support", "support123");
    String preprocessRequest = """
            {
              "rawText":"### 场景标题：按协议号查询代发明细\\n- 结果字段：代发批次号、交易金额",
              "sourceType":"PASTE_MD",
              "sourceName":"payroll-material-a.md",
              "preprocessMode":"RULE_ONLY",
              "autoCreateDrafts":true
            }
            """;

    MvcResult preprocessResult = mockMvc.perform(post("/api/import/preprocess")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(preprocessRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importBatchId").isString())
            .andExpect(jsonPath("$.materialId").isString())
            .andReturn();

    JsonNode preprocessJson = objectMapper.readTree(preprocessResult.getResponse().getContentAsString());
    String taskId = preprocessJson.path("importBatchId").asText("");
    String materialId = preprocessJson.path("materialId").asText("");

    mockMvc.perform(get("/api/import/tasks/{taskId}", taskId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(taskId))
            .andExpect(jsonPath("$.materialId").value(materialId));

    SourceMaterialPO material = sourceMaterialMapper.findById(materialId).orElseThrow();
    assertThat(material.getSourceName()).isEqualTo("payroll-material-a.md");
    assertThat(material.getSourceType()).isEqualTo("PASTE_MD");
    assertThat(material.getRawText()).contains("代发批次号");
    assertThat(material.getTextFingerprint()).isNotBlank();
}
```

- [ ] **Step 2: Write the failing runtime datasource config test**

Create `DataSourceRuntimeConfigTest` to pin the new baseline:

```java
@SpringBootTest
class DataSourceRuntimeConfigTest {

    @Autowired
    private Environment environment;

    @Test
    void shouldUseMysqlCompatibleDatasourceDefaultsOutsideTestProfile() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:mysql://");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("com.mysql.cj.jdbc.Driver");
    }
}
```

- [ ] **Step 3: Run the targeted tests to verify red**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,DataSourceRuntimeConfigTest test`

Expected:

- `ImportTaskApiIntegrationTest` FAILS because `materialId` and `SourceMaterialMapper` do not exist yet.
- `DataSourceRuntimeConfigTest` FAILS because runtime defaults still point to `jdbc:h2:mem:...`.

### Task 2: Switch runtime datasource defaults to MySQL-compatible configuration

**Files:**

- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Replace runtime defaults in `application.yml`**

Update the datasource section to use environment-driven MySQL-compatible defaults:

```yml
spring:
  datasource:
    url: ${CALIBER_DB_URL:jdbc:mysql://127.0.0.1:3306/caliber?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai}
    driver-class-name: ${CALIBER_DB_DRIVER:com.mysql.cj.jdbc.Driver}
    username: ${CALIBER_DB_USERNAME:caliber}
    password: ${CALIBER_DB_PASSWORD:caliber}
```

- [ ] **Step 2: Keep the test profile explicit**

Leave `application-test.yml` on isolated in-memory datasource so the TDD loop remains deterministic:

```yml
spring:
  datasource:
    url: jdbc:h2:mem:caliber_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

- [ ] **Step 3: Re-run the runtime config test**

Run:

- `cd backend && mvn -q -Dtest=DataSourceRuntimeConfigTest test`

Expected:

- PASS, proving the non-test runtime baseline no longer points to H2.

### Task 3: Add `Source Material` schema and JPA mappings

**Files:**

- Create: `backend/src/main/resources/db/migration/V13__add_source_material.sql`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/SourceMaterialPO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/SourceMaterialMapper.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po/ImportTaskPO.java`

- [ ] **Step 1: Add the Flyway migration**

Create `V13__add_source_material.sql`:

```sql
CREATE TABLE caliber_source_material (
    material_id VARCHAR(64) PRIMARY KEY,
    source_type VARCHAR(32),
    source_name VARCHAR(255),
    raw_text TEXT,
    text_fingerprint VARCHAR(128) NOT NULL,
    operator VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_source_material_source_type_updated
    ON caliber_source_material (source_type, updated_at);

CREATE INDEX idx_source_material_fingerprint
    ON caliber_source_material (text_fingerprint);

ALTER TABLE caliber_import_task
    ADD COLUMN material_id VARCHAR(64);

CREATE INDEX idx_import_task_material_id
    ON caliber_import_task (material_id);

ALTER TABLE caliber_import_task
    ADD CONSTRAINT fk_import_task_material
    FOREIGN KEY (material_id) REFERENCES caliber_source_material (material_id);
```

- [ ] **Step 2: Add the entity and mapper**

Create `SourceMaterialPO.java`:

```java
@Entity
@Table(name = "caliber_source_material",
        indexes = {
                @Index(name = "idx_source_material_source_type_updated", columnList = "source_type,updated_at"),
                @Index(name = "idx_source_material_fingerprint", columnList = "text_fingerprint")
        })
public class SourceMaterialPO {

    @Id
    @Column(name = "material_id", nullable = false, length = 64)
    private String materialId;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Lob
    @Column(name = "raw_text")
    private String rawText;

    @Column(name = "text_fingerprint", nullable = false, length = 128)
    private String textFingerprint;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

Create `SourceMaterialMapper.java`:

```java
public interface SourceMaterialMapper extends JpaRepository<SourceMaterialPO, String> {
}
```

- [ ] **Step 3: Link `ImportTaskPO` to `material_id`**

Add the field:

```java
@Column(name = "material_id", length = 64)
private String materialId;
```

- [ ] **Step 4: Run the import-material test to keep it red-green scoped**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest test`

Expected:

- Still FAIL, but now the schema and repository exist; failure should remain at service/API behavior, not missing table metadata.

### Task 4: Persist material records during import and expose `materialId`

**Files:**

- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/ImportTaskDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/PreprocessResultDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportTaskCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/ImportCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/ImportTaskQueryAppService.java`

- [ ] **Step 1: Add `materialId` to the DTO contracts**

Update the records:

```java
public record ImportTaskDTO(
        String taskId,
        String materialId,
        String status,
        Integer currentStep,
        String sourceType,
        String sourceName,
        String operator,
        String rawText,
        Boolean qualityConfirmed,
        Boolean compareConfirmed,
        JsonNode preprocessResult,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
}
```

```java
public record PreprocessResultDTO(
        String caliberImportJson,
        String mode,
        JsonNode global,
        List<JsonNode> scenes,
        JsonNode quality,
        List<String> warnings,
        Double confidenceScore,
        String confidenceLevel,
        Boolean lowConfidence,
        Long totalElapsedMs,
        List<StageTimingDTO> stageTimings,
        List<PreprocessSceneDraftDTO> sceneDrafts,
        String importBatchId,
        String materialId
) {
}
```

- [ ] **Step 2: Create or update `Source Material` inside task start**

In `ImportTaskCommandAppService.start(...)`, create a material record before saving the task:

```java
SourceMaterialPO material = resolveMaterial(po, now);
material.setSourceType(trimToNull(cmd.sourceType()));
material.setSourceName(trimToNull(cmd.sourceName()));
material.setRawText(cmd.rawText());
material.setOperator(trimToNull(cmd.operator()));
material.setTextFingerprint(buildFingerprint(cmd));
material.setUpdatedAt(now);
sourceMaterialMapper.save(material);

po.setMaterialId(material.getMaterialId());
```

Use a helper like:

```java
private String buildFingerprint(PreprocessImportCmd cmd) {
    String payload = String.join("|",
            trimToEmpty(cmd.sourceType()),
            trimToEmpty(cmd.sourceName()),
            trimToEmpty(cmd.rawText()));
    return DigestUtils.sha256Hex(payload);
}
```

- [ ] **Step 3: Thread `materialId` through query and preprocess responses**

Update `toDTO(...)` in both import task services and `decorateResult(...)` in `ImportCommandAppService` so the same `materialId` appears in:

- `POST /api/import/preprocess`
- `POST /api/import/preprocess-llm`
- `GET /api/import/tasks/{taskId}`

- [ ] **Step 4: Run the targeted verification**

Run:

- `cd backend && mvn -q -Dtest=ImportTaskApiIntegrationTest,DataSourceRuntimeConfigTest test`

Expected:

- PASS, with preprocess response returning `materialId`, task detail returning the same ID, and source material row persisted independently.

### Task 5: Verify Flyway and runtime startup against the new baseline

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/architecture/features/iteration-01-knowledge-production/01-材料接入与来源接入契约登记.md`

- [ ] **Step 1: Update handoff status**

After implementation, change the delivery-status item so the database-plan link is marked as landed, and record the first completed code slice as “Source Material + Import Task link landed”.

- [ ] **Step 2: Run fresh startup verification**

Run:

- `cd backend && mvn -q -DskipTests package`
- `cd backend && CALIBER_DB_URL='jdbc:mysql://127.0.0.1:3306/caliber?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' CALIBER_DB_USERNAME=caliber CALIBER_DB_PASSWORD=caliber ./mvnw spring-boot:run`

Expected:

- Package succeeds.
- Application starts on `8080` using MySQL-compatible datasource properties and Flyway applies `V13__add_source_material.sql`.
