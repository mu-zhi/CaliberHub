原创 awaha *2025年03月04日 20:04*

# 【关键词】Text2SQL；NL2SQL；数据集；WikiSQL；Spider；Spider 2；SParC；CoSQL；BIRD

**🎯** **自然语言与数据库的沟通困境**

在当今数字化时代，数据库作为数据存储和管理的核心工具，广泛应用于各个领域。无论是企业的数据管理、科研的数据研究，还是日常的信息查询，我们都离不开数据库。然而，传统的数据库查询语言 ——SQL，却像一道门槛，横亘在普通用户与数据库之间。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/0s2RLufxO2NfP6dpUf4BSXlrmAksEv6NtS5ZNIYLrDuR3fYThg257NG8nicVIgbPpGiaGXs8muPZoux0cjYe8pbg/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

想象一下，你是一位企业的市场分析师，想要从公司的数据库中获取过去一个月内，购买次数超过 5 次且消费金额大于 1000 元的客户信息。面对 SQL 复杂的语法规则，你可能会感到无从下手。编写 SQL 语句需要熟悉各种关键字、函数和语法结构，稍有不慎就会出错。对于不具备专业编程知识的人来说，这无疑是一项艰巨的任务。

即使是对于有一定编程基础的人，编写复杂的 SQL 查询也并非易事。当涉及到多个表的关联查询、复杂的条件筛选和数据聚合时，SQL 语句会变得冗长而复杂，难以理解和维护。而且，不同的数据库系统可能还存在一些语法差异，这进一步增加了学习和使用的难度。

**🎯Text2SQL / NL2SQL 是什么**

Text2SQL / NL2SQL，即 “Text-to-SQL” 或 “Natural Language to SQL” ，顾名思义，就是将自然语言（Natural Language，NL）问题转化为在关系型数据库中可以执行的结构化查询语言（Structured Query Language，SQL），它也是传统 NLP 的任务之一。它的核心在于让用户能够用日常的语言与数据库进行交互，而无需编写复杂的 SQL 语句。

在实际产业界应用中，Text2SQL往往作为核心引擎，提供用户数据挖掘、报告处理的自然交互方式 ，通过从生成的 SQL 语句，拿到生产的实际数据，输出方式可能为表格、图表、报告等。

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

虚线框内，是自动处理的过程，用户无感知，用户只会体会便是：

输入左侧的要求，便得到最右侧的图表。

**🎯数据集**

众所周知，数据集是 AI 应用的原料，处于算力、算法、数据三要素之一。尤其是现在，算力、算法已经取得了重大进展，且可以直接调用 API 能力，那针对更多垂直领域，要发展 AI 应用， 数据便是最重要的资源 。

Text2SQL 数据集分类：

- 根据包含领域数量，数据集分为单领域和多领域。
- 根据每个数据库包含表的数量，数据集分为单表和多表模式。在多表模式中，SQL生成涉及到表格的选择。
- 根据问题复杂度，数据集分为简单问题和复杂问题模式，其中问题复杂度由SQL查询语句涉及到的关键词数量、嵌套层次、子句数量等确定。
- 根据完整SQL生成所需轮数，数据集分为单轮和多轮。
- 若SQL生成融进渐进式对话，则数据集增加“结合对话”标记。当前只有CoSQL数据集是融进对话的数据集。

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

业界常用数据集：

# WikiSQL

- 提出方：Salesforce
- 2017
- 包含了 24,241 张表，80,645条自然语言问句及相应的SQL语句
- https://github.com/salesforce/WikiSQL
- https://arxiv.org/pdf/1709.00103

We evaluate using the execution accuracy metric Accex = Nex / N. One downside of Accex is that it is possible to construct a SQL query that does not correspond to the question but nevertheless obtains the same result.

For example, the two queries SELECT COUNT(name) WHERE SSN = 123 and SELECT COUNT(SSN) WHERE SSN = 123 produce the same result if no two people with different names share the SSN 123. Hence, we also use the logical form accuracy Acclf = Nlf / N. However, as we showed in Section 2.2, Acclf incorrectly penalizes queries that achieve the correct result but do not have exact string match with the ground truth query. Due to these observations, we use both metrics to evaluate the models.

# Spider

- Spider：https://arxiv.org/pdf/1809.08887
- https://yale-lily.github.io/spider
- https://spider2-sql.github.io/
- 2018年9月，耶鲁大学提出的多数据库、多表、单轮查询的Text-to-SQL数据集，也是业界公认难度最大的大规模跨领域评测榜单，包含了10181个自然语言问题，5693个SQL语句，涉及138个不同领域的200多个数据库， 难易程度分为：简单、中等、困难、特别困难 。 2024年2月 ，耶鲁大学开源了Spider1.0排行榜单的test数据集，并且他们将在3月开源 Spider 2.0 数据集。
- Spider 2.0，包含来自企业数据库的 632 个真实世界的文本到 SQL 任务。这些数据库通常有 1000 多列，来自云或本地系统，如 BigQuery、Snowflake 和 PostgreSQL。解决这些任务需要模型理解数据库元数据、项目特定语言和项目代码，导航复杂的 SQL 环境并处理长上下文。模型必须执行高级推理并生成多样化的 SQL 查询，有时超过 100 行，超过传统的文本到 SQL 挑战。
- 下图为Spider 1难易划分举例：

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

# SParC

- https://yale-lily.github.io/sparc
- Yale & Salesforce

***SParC*** is a dataset for cross-domain **S** emantic **Par** sing in **C** ontext. It is the context-dependent/multi-turn version of the Spider task, a complex and cross-domain text-to-SQL challenge. SParC consists of 4,298 coherent question sequences (12k+ unique individual questions annotated with SQL queries annotated by 14 Yale students), obtained from user interactions with 200 complex databases over 138 domains.

下图为数据集示例：

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

# CoSQL

# 重点在于，系统可以从用户的对话，不断引导确认用户的需求，而非只从简单的一句话

- https://yale-lily.github.io/cosql

***CoSQL*** is a corpus for building cross-domain **Co** nversational text-to- **SQL** systems. It is the dialogue version of the Spider and SParC tasks. CoSQL consists of 30k+ turns plus 10k+ annotated SQL queries, obtained from a Wizard-of-Oz collection of 3k dialogues querying 200 complex databases spanning 138 domains. Each dialogue simulates a real-world DB query scenario with a crowd worker as a user exploring the database and a SQL expert retrieving answers with SQL, clarifying ambiguous questions, or otherwise informing of unanswerable questions.

下图为数据集示例：

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

# BIRD - 202305

- https://bird-bench.github.io/
- https://github.com/AlibabaResearch/DAMO-ConvAI/tree/main/bird

2023年5月，香港大学和阿里巴巴提出了一个大规模 跨域数据集BIRD ，其中包含超过12751个独特的问题 SQL、95个大数据库，总大小为33.4GB。它还 涵盖区块链、曲棍球、医疗保健和教育等超过37个专业领域 。

BIRD-SQL is the first cross-domain large-scale benchmark specifically designed to bridge the gap between academic research and real-world applications in the field of text-to-SQL parsing. While models such as Codex and ChatGPT have demonstrated remarkable performance, existing benchmarks such as Spider and WikiSQL concentrate primarily on database schema, leaving database contents largely unexplored. Realizing this limitation, we set out to create a comprehensive benchmark that delves deeper into database values, ultimately unveiling new challenges and opportunities for developments in the text-to-SQL domain.

BIRD-SQL is distinguished by its large dataset, which includes **12,751** text-to-SQL pairs, **95** databases encompassing **37** professional domains, and a total size of **33.4 GB**. By highlighting database values, BIRD-SQL draws attention to new challenges, such as external knowledge, dirty data, and SQL efficiency in vast databases. In order to generate accurate SQL queries, models must not only conduct semantic parsing but also comprehend database values.

Valid Efficiency Score (VES) Evaluation (time-mainly)

该基准测试还引入了一个 新的度量标准——有效性评分（Valid Efficiency Score，VES） ，以评估生成的SQL语句的执行效率。这是首个将效率纳入考虑的文本到SQL基准测试，为在大规模和嘈杂数据库背景下实现更高效的查询方法提供推动。

持续分享 AI 见闻和思考，包括但不限于技术、工具、应用…

您的每一个评论、点赞、转发，对我都很重要~

关注我们， 第一时间掌握 AI 相关信息、行业动态解读 ：

修改于 2025年03月05日

继续滑动看下一个

AI爱好者小站

向上滑动看下一个

拖拽到此处完成下载

图片将完成下载

AIX智能下载器
