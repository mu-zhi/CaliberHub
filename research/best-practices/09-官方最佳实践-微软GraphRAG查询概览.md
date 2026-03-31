# 官方最佳实践-微软GraphRAG查询概览

本文整理微软官方 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 查询概览页面中，与数据直通车运行时检索模式直接相关的部分。

## 1. 官方来源

1. 来源：[微软官方 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 查询概览](https://microsoft.github.io/graphrag/query/overview/)
2. 来源方：微软官方 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 文档
3. 访问日期：2026-03-12

## 2. 官方页面讲了什么

这个页面把 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 的查询引擎拆成 3 类能力：

1. `Local Search（局部搜索，Local Search）`
2. `Global Search（全局搜索，Global Search）`
3. `DRIFT Search（DRIFT 搜索，Dynamic Reasoning and Inference with Flexible Traversal）`

这个拆法对我们非常重要，因为它告诉我们：企业问题不是都该走同一种检索模式。

## 3. 三种模式的核心区别

### 3.1 `Local Search（局部搜索，Local Search）`

这类问题围绕一个具体对象、一个局部关系网或一段局部路径展开。

对数据直通车来说，最典型的问题就是：

1. 查询客户某年基金交易
2. 某个身份证号最后怎么落到某张交易表
3. 某个账户为什么会命中这个口径

这类问题的关键不是全域总结，而是把局部路径、局部规则、局部证据找对。

### 3.2 `Global Search（全局搜索，Global Search）`

官方页面明确说，这类模式是通过全局范围的社区报告做回答，适合需要把数据集整体看明白的问题。

对数据直通车来说，这更像：

1. 当前零售客户类高频场景有哪些共性问题
2. 财富管理领域里哪些场景最依赖历史补齐
3. 当前哪些领域存在最高的规则复用机会

这类问题不直接服务单次取数，而更适合运营分析、治理盘点和主题归纳。

### 3.3 `DRIFT Search（DRIFT 搜索，Dynamic Reasoning and Inference with Flexible Traversal）`

官方页面把它定义成：在局部查询里引入社区信息，逐步扩展问题起点，让系统能得到更广的一组事实。

对数据直通车来说，这类问题往往是：

1. 为什么这次结果和上次不一样
2. 这条路径为什么会从当前库漂到历史库
3. 某个场景升级后，哪些规则和证据一起变了

这类问题的特点不是“更大范围”，而是“从一个局部问题逐步扩到版本、规则、证据和相邻路径”。

## 4. 对数据直通车的直接启发

### 4.1 运行时检索不能只有一种模式

如果只做一种统一检索，系统很容易出现两类错误：

1. 本该查局部路径的问题，被错误放大成全局摘要。
2. 本该做全局归纳的问题，只返回了几个局部表和字段。

所以我们的主方案里把检索输入先拆成业务意图、候选领域、候选场景、时间语义、条件实体和风险信号，是有官方依据的。

### 4.2 业务子图更适合服务局部问题

对数据直通车当前阶段来说，运行时的核心产物仍应是问题相关业务子图。它最适合承载 `Local Search（局部搜索，Local Search）` 和一部分 `DRIFT Search（DRIFT 搜索，Dynamic Reasoning and Inference with Flexible Traversal）`。

### 4.3 全局类问题应更多服务治理和数据地图

`Global Search（全局搜索，Global Search）` 更适合放到数据地图、治理盘点、影响分析和全局问答，而不是直接放到当前 `NL2SQL（自然语言转 SQL，Natural Language to SQL）` 主链路里。

## 5. 我们不直接照搬的地方

微软官方 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 是面向通用知识问答方法总结的。落到数据直通车时，我们还要补 4 件事：

1. 场景边界
2. 时间语义
3. 证据追溯
4. 版本治理

也就是说，我们不能只学“哪种查询模式”，还要补上金融取数场景要求的约束和审计能力。

## 6. 当前最值得吸收的结论

这份官方文档最值得直接带回来的结论是：

企业级 `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 不存在一种通吃的检索模式，至少要区分局部问题、全局问题和逐步扩展问题。
