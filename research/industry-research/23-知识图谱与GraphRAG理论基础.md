# 知识图谱与 GraphRAG 理论基础

本文作为“知识图谱 / 数据地图上游方案”与“`NL2SQL（自然语言转 SQL，Natural Language to SQL）` 下游消费方案”之间的理论衔接页，聚焦三个问题：

1. 为什么企业场景需要知识图谱。
2. `GraphRAG（图检索增强生成，Graph Retrieval-Augmented Generation）` 与普通 `RAG（检索增强生成，Retrieval-Augmented Generation）` 的边界。
3. 在数据直通车中，理论如何映射到“分层召回、统一寻路、门禁治理”的工程实现。

---

## 1. 知识图谱在本项目中的定位

数据直通车上游不是“文档索引系统”，而是“可治理知识底座”：

1. 用 `业务场景（Business Scene）` 收敛查询边界。
2. 用 `字段概念（Field Concept）`、`组合概念（Composite Concept）`、`表间关联关系对象（Join Relation Object）` 组织可解释路径。
3. 用 `证据片段（Evidence Fragment）` 与 `版本快照（Version Snapshot）` 提供可审计依据。

---

## 2. GraphRAG 与普通 RAG 的差异

| 维度   | 普通 RAG | GraphRAG         |
| ---- | ------ | ---------------- |
| 核心对象 | 文本块    | 节点 + 关系 + 文本     |
| 主要能力 | 召回相关段落 | 召回关系路径与结构证据      |
| 可解释性 | 片段级    | 路径级（可解释“为什么这样连”） |
| 治理能力 | 文本版本为主 | 对象级版本、证据、影响分析    |

在数据直通车语境下，普通 `RAG` 更适合背景说明召回；`GraphRAG` 更适合业务边界判定、路径规划与证据追溯。

---

## 3. 与当前工程方案的映射关系

1. 分层召回：语义层采用 `Semantic Retrieval（语义召回）`，元数据层采用 `Deterministic Lookup（确定性定位）`。
2. 统一寻路：召回结果统一进入 `BFS（广度优先搜索，Breadth-First Search）` 路径组装、评分与裁剪。
3. 门禁发布：通过 `conf_score（置信度分值）`、合法性、边界契约与版本冲突检查收口到 `allow / need_approval / deny`。

---

## 4. 相关阅读

1. [07-官方最佳实践总览](07-官方最佳实践总览.md)
2. [10-官方最佳实践-Neo4j-GraphRAG指南](10-官方最佳实践-Neo4j-GraphRAG指南.md)
3. [12-官方最佳实践-微软GraphRAG官方仓库与方法说明](12-官方最佳实践-微软GraphRAG官方仓库与方法说明.md)
