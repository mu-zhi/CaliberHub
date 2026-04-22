# 02f-RAG 预处理实验适配与候选回写测试报告

> 对应特性文档：`docs/architecture/features/iteration-01-knowledge-production/02f-rag-预处理实验适配与候选回写.md`
> 对应实施计划：`docs/plans/2026-04-22-rag-preprocess-experiment-adapter-implementation-plan.md`
> 日期：2026-04-22
> 状态：passed

## 一、测试目标

验证知识生产预处理链路已经接入统一 `Preprocess Experiment Adapter（预处理实验适配器）`，并满足三条硬边界：

1. 实验结果只回写候选层，不写正式治理资产。
2. 导入返回与流式事件都能暴露实验适配器摘要、引用与零正式写入标记。
3. 知识生产台能明确表达“实验候选仅进入候选层”。

## 二、执行记录

### 2.1 后端契约与导入链路

- 执行命令：
  - `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" sh "/Users/rlc/Library/Application Support/JetBrains/IntelliJIdea2026.1/plugins/maven/lib/maven3/bin/mvn" -q -Dtest=PreprocessExperimentAdapterContractTest,ImportCommandAppServiceTest,ImportPreprocessStreamApiIntegrationTest test`
- 结果：
  - `PreprocessExperimentAdapterContractTest` 通过，已锁定 `adapterName / referenceRefs / formalAssetWrites` 契约。
  - `ImportCommandAppServiceTest` 通过，已锁定单块导入与多块导入都挂载 `preprocessExperiment` 摘要。
  - `ImportPreprocessStreamApiIntegrationTest` 通过，流式返回中包含 `preprocessExperiment`、`referenceRefs` 与 `formalAssetWrites: []`。

### 2.2 前端知识生产台反馈

- 执行命令：
  - `cd frontend && NODE_OPTIONS="--require=<rollup wasm hook>" npm test -- src/components/knowledge/importLiveGraphState.test.js src/pages/KnowledgePage.render.test.jsx`
- 结果：
  - `importLiveGraphState.test.js` 通过，已锁定最终快照恢复时保留实验摘要。
  - `KnowledgePage.render.test.jsx` 通过，首屏已明确展示“实验候选仅进入候选层”，且不再回退到“正在等待首批实体”的旧空态文案。

## 三、覆盖结论

- 已覆盖：
  - 统一预处理实验适配器输入输出契约。
  - 导入返回与 SSE 事件的实验摘要透出。
  - 知识生产台的实验候选层边界提示。
- 未覆盖：
  - `ImportExperimentRunPO / ImportExperimentRunMapper` 持久化台账。本轮以现有导入任务返回与候选图反馈完成最小闭环，后续如需独立运行台账再补专门表结构。

## 四、遗留风险

1. 前端测试依赖本机 `rollup` 原生模块；本轮在验证时使用 `@rollup/wasm-node` 回退 hook 规避本机签名问题，仓库代码未引入该绕行逻辑。
2. 实验候选仍是 `LightRAG（开源 GraphRAG 检索框架，LightRAG）` 首个适配器实现，后续替换引擎时需复跑同一组契约测试，确保候选层边界不漂移。
