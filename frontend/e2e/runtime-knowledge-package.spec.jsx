import { expect, test } from "@playwright/test";

const publishedScene = {
  id: 1,
  sceneCode: "SCN_PAYROLL_DETAIL",
  sceneTitle: "代发明细查询",
  sceneType: "FACT_DETAIL",
  sceneDescription: "代发明细场景",
  domainId: 10,
  publishedAt: "2026-03-28T10:00:00Z",
  status: "PUBLISHED",
};

const sceneBundle = {
  plans: [{ planId: 1, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" }],
  coverages: [{ id: 1, coverageCode: "CVG-001", coverageTitle: "近一年代发覆盖", status: "FULL_MATCH", applicablePeriod: "近一年" }],
  policies: [],
  contractViews: [],
  sourceContracts: [],
  publishCheck: { publishReady: true, items: [] },
  versions: [{ id: 42, versionTag: "v1", publishedAt: "2026-03-28T10:00:00Z" }],
  inputSlots: [{ slotCode: "PROTOCOL_NBR", slotName: "协议号", identifierCandidatesJson: "[\"PROTOCOL_NBR\"]" }],
  outputContracts: [{ fieldsJson: "[\"协议号\",\"交易日期\",\"金额\"]" }],
  projection: { status: "READY", lastProjectedAt: "2026-03-28T10:00:00Z" },
};

const allowResult = {
  decision: "allow",
  reasonCode: "ALLOW",
  runtimeMode: "FULL_MATCH",
  degradeReasonCodes: ["ALLOW"],
  scene: { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询" },
  plan: { planId: 1, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" },
  coverage: { status: "FULL", matchedSegment: "2021-Q1" },
  policy: { decision: "allow" },
  contract: {
    visibleFields: ["协议号", "交易日期", "金额"],
    maskedFields: [],
    restrictedFields: [],
    forbiddenFields: [],
  },
  trace: { traceId: "trace_allow_001", snapshotId: 42, inferenceSnapshotId: 42, versionTag: "v1" },
  evidence: [{ evidenceCode: "EV_001", title: "代发交易说明", sourceAnchor: "§3.2" }],
  risk: { riskLevel: "LOW", riskReasons: [] },
  path: { resolutionSteps: ["场景命中", "方案选择", "覆盖校验"] },
};

test("renders candidate scene count and localized coverage status in the runtime workbench", async ({ page }) => {
  await page.route("**/*", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (!path.startsWith("/api/")) {
      await route.continue();
      return;
    }

    if (path === "/api/scenes") {
      await route.fulfill({ json: [publishedScene] });
      return;
    }
    if (path === "/api/plans") {
      await route.fulfill({ json: sceneBundle.plans });
      return;
    }
    if (path === "/api/coverage-declarations") {
      await route.fulfill({ json: sceneBundle.coverages });
      return;
    }
    if (path === "/api/policies") {
      await route.fulfill({ json: sceneBundle.policies });
      return;
    }
    if (path === "/api/contract-views") {
      await route.fulfill({ json: sceneBundle.contractViews });
      return;
    }
    if (path === "/api/source-contracts") {
      await route.fulfill({ json: sceneBundle.sourceContracts });
      return;
    }
    if (path === "/api/publish-checks/1") {
      await route.fulfill({ json: sceneBundle.publishCheck });
      return;
    }
    if (path === "/api/scenes/1/versions") {
      await route.fulfill({ json: sceneBundle.versions });
      return;
    }
    if (path === "/api/input-slot-schemas") {
      await route.fulfill({ json: sceneBundle.inputSlots });
      return;
    }
    if (path === "/api/output-contracts") {
      await route.fulfill({ json: sceneBundle.outputContracts });
      return;
    }
    if (path === "/api/graphrag/projection/1") {
      await route.fulfill({ json: sceneBundle.projection });
      return;
    }
    if (path === "/api/scene-search") {
      await route.fulfill({ json: { candidates: [{ sceneId: 1, sceneTitle: "代发明细查询" }], reasons: ["关键词命中"] } });
      return;
    }
    if (path === "/api/plan-select") {
      await route.fulfill({ json: { candidates: [{ planId: 1, planName: "代发明细方案", decision: "allow" }], reasons: ["首选方案命中"] } });
      return;
    }
    if (path === "/api/graphrag/query") {
      await route.fulfill({ json: allowResult });
      return;
    }
    await route.fulfill({ json: {} });
  });

  await page.goto("/#/runtime");
  await page.getByLabel("标识值").fill("P-20260328-001");
  await page.getByRole("button", { name: "生成知识包" }).click();

  await expect(page.getByText("1 个候选场景")).toBeVisible();
  await expect(page.getByText("完整覆盖").first()).toBeVisible();
  await expect(page.getByText("代发明细方案").first()).toBeVisible();
  await expect(page.getByText("trace_allow_001").first()).toBeVisible();
});
