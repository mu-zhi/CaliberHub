import { expect, test } from "@playwright/test";

test("real-service import live graph smoke", async ({ page }) => {
  await page.goto("/#/production/ingest");

  await expect(page.getByRole("button", { name: "载入样例" })).toBeVisible();
  await page.getByRole("button", { name: "载入样例" }).click();
  await page.getByTestId("knowledge-step-1-reopen").click();
  await page.getByTestId("knowledge-step-1-submit").click();

  await expect(page.getByRole("heading", { name: "最佳实践样例" })).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("heading", { name: "当前任务停留在抽取质量判断" })).toBeVisible({ timeout: 15000 });
  await expect(page.getByText(/场景队列：待处理 3 个/)).toBeVisible({ timeout: 15000 });
  await expect(page.getByText("导入明细：当前阶段 导入与草稿生成完成")).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("button", { name: "查看数据地图" })).toBeVisible({ timeout: 15000 });
});

test("real-service restore task works from history queue", async ({ page }) => {
  await page.goto("/#/production/ingest");

  await page.getByTestId("knowledge-step-1-reopen").click();
  const restoreButton = page.locator('[data-testid^="knowledge-restore-task-"]').first();
  await expect(restoreButton).toBeVisible({ timeout: 15000 });
  await restoreButton.click();

  await expect(page.getByRole("heading", { name: "最佳实践样例" })).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("heading", { name: "当前任务停留在抽取质量判断" })).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole("button", { name: "查看数据地图" })).toBeVisible({ timeout: 15000 });
});

test("real-service runtime workbench smoke", async ({ page }) => {
  await page.goto("/#/runtime");

  await expect(page.getByRole("heading", { name: "运行决策台" })).toBeVisible();
  await expect(page.getByRole("combobox", { name: "选择场景" })).toBeVisible();
  await page.getByRole("combobox", { name: "选择场景" }).selectOption({ label: "Step 2：按需补查历史明细表" });
  await page.getByLabel("标识值").fill("P-20260328-001");
  await page.getByRole("button", { name: "生成知识包" }).click();

  await expect(page.getByRole("heading", { name: "知识包摘要" })).toBeVisible();
  await expect(page.getByText("Step 2：按需补查历史明细表 / 取数方案1")).toBeVisible();
  await expect(page.getByText("完整覆盖", { exact: true })).toBeVisible();
  await expect(page.getByText(/KP-[A-Z0-9]+ \/ 快照 4 \/ 推理快照 4/)).toBeVisible();
  await expect(page.getByRole("link", { name: "去数据地图查看定位" })).toBeVisible();
});

test("real-service approval export page smoke", async ({ page }) => {
  const ctx = encodeURIComponent(JSON.stringify({
    source_workbench: "runtime",
    target_workbench: "approval",
    intent: "submit_approval",
    scene_code: "SCN-8FAD96CD941F",
    trace_id: "trace_runtime_20260401_approval",
    lock_mode: "latest",
  }));
  await page.goto(`/#/approval?ctx=${ctx}`);

  await expect(page.getByRole("heading", { name: "审批与导出" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "导出记录" })).toBeVisible();
  await expect(page.getByRole("button", { name: "改为脱敏导出" })).toBeVisible();
  const latestRecord = page.locator(".workbench-pane").last().locator("article");
  await expect(latestRecord.first()).toBeVisible({ timeout: 15000 });
  await expect(page.getByText(/SPEC-/).first()).toBeVisible({ timeout: 15000 });
});
