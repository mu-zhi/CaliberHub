import { expect, test } from "@playwright/test";

function buildImportStreamBody() {
  return [
    'event: start\ndata: {"taskId":"task-live-001"}',
    'event: stage\ndata: {"stageKey":"normalize","stageName":"结果归一","message":"首批候选实体已生成","percent":60,"elapsedMs":900}',
    'event: graph_patch\ndata: {"graphId":"task-live-001:material-live-001","patchSeq":1,"summary":"首批补丁","focusNodeIds":["SC-001"],"addedNodes":[{"id":"SC-001","nodeType":"CANDIDATE_SCENE","label":"代发明细查询"}],"addedEdges":[]}',
    'event: graph_patch\ndata: {"graphId":"task-live-001:material-live-001","patchSeq":2,"summary":"第二批补丁","focusNodeIds":["PLN-002"],"addedNodes":[{"id":"PLN-002","nodeType":"CANDIDATE_PLAN","label":"历史补查"}],"addedEdges":[{"id":"EDGE-002","relationType":"SCENE_HAS_PLAN","sourceId":"SC-001","targetId":"PLN-002"}]}',
    'event: done\ndata: {"importBatchId":"task-live-001","materialId":"material-live-001","totalElapsedMs":1600,"scenes":[]}',
    "",
  ].join("\n\n");
}

test("keeps the live candidate graph visible after the import stream completes", async ({ page }) => {
  await page.route("**/*", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();
    if (!path.startsWith("/api/")) {
      await route.continue();
      return;
    }

    if (path === "/api/domains") {
      await route.fulfill({ json: [{ id: 1, domainName: "人力域", domainCode: "HR" }] });
      return;
    }
    if (path === "/api/import/tasks") {
      await route.fulfill({ json: [] });
      return;
    }
    if (path === "/api/import/preprocess-stream" && method === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "text/event-stream",
        body: buildImportStreamBody(),
      });
      return;
    }
    await route.fulfill({ json: {} });
  });

  await page.goto("/#/production/ingest");
  await page.getByRole("button", { name: "填入最佳实践样例" }).click();
  await page.getByRole("button", { name: "导入并生成草稿" }).click();

  await expect(page.getByRole("heading", { name: "候选实体图谱" })).toBeVisible();
  await expect(page.getByLabel("候选实体图谱画布").getByText("代发明细查询")).toBeVisible();
  await expect(page.getByLabel("候选实体图谱画布").getByText("历史补查")).toBeVisible();
  await expect(page.getByText("导入执行明细")).toBeVisible();
});
