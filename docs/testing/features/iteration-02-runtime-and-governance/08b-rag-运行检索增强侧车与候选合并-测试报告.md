# 08b-RAG 运行检索增强侧车与候选合并测试报告

> 对应特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/08b-rag-运行检索增强侧车与候选合并.md`
> 对应实施计划：`docs/plans/2026-04-22-rag-runtime-retrieval-sidecar-implementation-plan.md`
> 日期：2026-04-22
> 状态：passed

## 一、测试目标

验证统一 `Retrieval Experiment Adapter（运行检索实验适配器）` 已接入知识包主链路，并满足：

1. 实验侧车只补候选场景、候选证据和引用，不直接给出正式 `decision（决策）`。
2. 知识包返回实验调试块，但正式 `allow / need_approval / deny` 仍由原链路给出。
3. 运行决策台能展示实验检索调试信息。

## 二、执行记录

### 2.1 后端运行契约与知识包接口

- 执行命令：
  - `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" sh "/Users/rlc/Library/Application Support/JetBrains/IntelliJIdea2026.1/plugins/maven/lib/maven3/bin/mvn" -q -Dtest=RetrievalExperimentAdapterContractTest,SceneQueryAppServiceTest,KnowledgePackageApiIntegrationTest test`
- 结果：
  - `RetrievalExperimentAdapterContractTest` 通过，锁定 `candidateScenes` 非空、`decision == null` 与 `referenceRefs` 非空。
  - `SceneQueryAppServiceTest` 通过，锁定实验候选仅作为 hint，不参与正式决策。
  - `KnowledgePackageApiIntegrationTest` 通过，知识包已返回 `trace.retrievalAdapter / retrievalStatus / fallbackToFormal` 与 `experiment` 调试块。

### 2.2 前端运行决策台调试块

- 执行命令：
  - `cd frontend && NODE_OPTIONS="--require=<rollup wasm hook>" npm test -- src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
- 结果：
  - 交互测试通过，已锁定 `实验检索调试` 区、`1 个候选场景`、`候选引用 2 条` 与 `LightRAG` 适配器展示。
  - 澄清分支、子问题二次检索与返回修改查询链路无回归。

## 三、覆盖结论

- 已覆盖：
  - 运行实验适配器契约。
  - 知识包实验调试块与追踪字段。
  - 运行决策台最小可见反馈。
- 未覆盖：
  - 真实 `scene hit@k` / `precision@k` 离线评测，本轮交给 `08d` 专题承接。

## 四、遗留风险

1. 首轮 `LightRAG` 适配器仍为启发式读模型实现，后续接入真实 GraphRAG 服务时需要复跑本报告中的契约与交互回归。
2. 前端验证同样依赖本机 `rollup` 的 wasm 回退 hook；仓库正式代码未引入该测试期 workaround。
