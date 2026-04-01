# 03 Asset Modeling and Governance Editing Test Report

- Feature doc: `docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md`
- Plan: `docs/plans/2026-03-31-governance-asset-modeling-implementation-plan.md`
- Scope in this round: backend Task 1 to Task 4

## Acceptance Boundary

| ID | Area | Gate Condition | Expected Result |
| --- | --- | --- | --- |
| AC-01 | `Dictionary` | A scene draft cannot be published when no scene-scoped dictionary asset exists. | Publish is blocked with validation error and missing `Dictionary` in message. |
| AC-02 | `Identifier Lineage` | A scene draft cannot be published when no scene-scoped identifier-lineage asset exists. | Publish is blocked with validation error and missing `Identifier Lineage` in message. |
| AC-03 | `Time Semantic Selector` | A scene draft cannot be published when no scene-scoped time-semantic-selector asset exists. | Publish is blocked with validation error and missing `Time Semantic Selector` in message. |
| AC-04 | Scene diff payload | Scene diff API must expose governance diff blocks for the three assets. | `/api/scenes/{id}/diff` returns `dictionaryChanges`, `identifierLineageChanges`, and `timeSemanticSelectorChanges`. |

## Execution Evidence

1. Boundary verification command:
   - `rg -n "Dictionary|Identifier Lineage|Time Semantic Selector" docs/testing/features/iteration-01-knowledge-production/03-asset-modeling-and-governance-editing-test-report.md`
   - Result: PASS, all three first-class governance assets are explicitly listed in acceptance rows.
2. Initial red-phase command:
   - `cd /Users/rlc/Code/CaliberHub/backend && mvn -q -Dtest=SceneCommandAppServiceTest,SceneApiIntegrationTest test`
   - Result: FAIL as expected before full implementation.
   - Key failures:
     - `SceneApiIntegrationTest.shouldExposeDictionaryIdentifierLineageAndTimeSemanticDiffBlocks`: missing `$.dictionaryChanges`.
     - `SceneCommandAppServiceTest.shouldRejectPublishWhenDictionaryIdentifierLineageAndTimeSemanticSelectorMissing`: publish gate not yet wired to required governance assets.
3. Final verification command:
   - `cd /Users/rlc/Code/CaliberHub/backend && mvn -q -Dtest=SceneCommandAppServiceTest,SceneApiIntegrationTest test`
   - Result: PASS.

## Final Gate Status

- AC-01 (`Dictionary` publish gate): PASS
- AC-02 (`Identifier Lineage` publish gate): PASS
- AC-03 (`Time Semantic Selector` publish gate): PASS
- AC-04 (diff payload includes `dictionaryChanges` / `identifierLineageChanges` / `timeSemanticSelectorChanges`): PASS
