原创 awaha *2025年03月07日 20:05*

# 【关键词】Text2SQL；NL2SQL；LLM；RAG；Agent；ReFoRCE + o1-preview

话分两头，前文写了 Text2SQL 的核心应用方式和 业内公认的数据集:

[Text2SQL / NL2SQL（一）数据集：自然语言与数据库的“翻译桥梁”](https://mp.weixin.qq.com/s?__biz=MzU4NjY4MzA4Ng==&mid=2247483790&idx=1&sn=a6260de4910a9495531552c7c22e20e1&scene=21#wechat_redirect)

接下来分享在数据集上进行探索的实现方法介绍，笔者才疏，仓促成文，有错漏敬请读者留言指出，十分感激！

Text2SQL 话题会一直更新，更多更新内容请关注本公众号！

**🎯实现方法发展简介**

**跟 AI 发展类似，Text-to-SQL 的研究由来已久，可以追溯到上世纪60年代，经历过起起伏伏，本文跳过“古老”的历史，直接从最近的热点方法讲起，下面是一个发展简图：**

**![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/0s2RLufxO2OptwM9ibM1cVDsc8AnyEXgWAFiaEwTlkth6iahYXYaDj1EL7nH7CIgMt58655SeO28gibC6ib3VVotynQ/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)**

2015 年以来该话题研究再度热门起来，最初是简单的 解析树和基于规则 的方法。

快进到 2019 年，继长短时记忆网络（LSTMs）后，Transformer 大受欢迎， Encoder-decoder-based 的方法如 SQL sketch 也备受关注。

2021 年，在 Text-to-SQL 语料库上对 BERT 、RoBERTa 这类现成的预训练语言模型进行 微调 变得流行。

随着像 ChatGPT（闭源）和 Llama 2/3（开源）、DeepSeek（开源）、Claude（闭源）等大语言模型（ LLMs ）的兴起，基于 LLM 在 Text-to-SQL 语料库的应用几乎成了解决该问题的实际标准，方法有 微调、RAG、Agent…

**🎯举个例子，看看业内最新成果**

根据 Spider 2 的榜单，链接：https://spider2-sql.github.io/

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/0s2RLufxO2OptwM9ibM1cVDsc8AnyEXgWNG1LZ40Zia7WolnPq1fxdARrNia5VKP2IB3rOjibkDrru9IHTRlEu28IQ/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

榜单前十，均是在大模型的基础上做的工作 。

有 2个中国大模型进入了榜单 ：DeepSeek-V3 和 Qwen2.5-Coder-32B-Instruct

顺道一提，从 2025.1 的最新结果来看， 最好分值也只有31.26 ，一方面说明 Spider 2 数据集难度和领先性，而且并没有被 LLM 拟合，另一方面也说明 AI 在 Spider 2 任务上仍面临着很多挑战。

> Notably, even the advanced LLMs-o1-preview solve only 17.1% of Spider 2.0 tasks. For widely-used models like GPT-4o, the success rate is only 10.1% on Spider 2.0 tasks, compared to 86.6% on Spider 1.0, underscoring the substantial challenges posed by Spider 2.0.
>
> https://spider2-sql.github.io/

**🎯举个例子，看看榜单 TOP1：ReFoRCE + o1-preview**

下面分享下 Spider 2 排行第一的 ReFoRCE + o1-preview，文章链接：https://arxiv.org/abs/2502.00675

ReFoRCE，即为 SelfRe finement Agent with  Fo rmat  R estriction and  C olumn  E xploration (ReFoRCE) workflow.

使用 chatGPT o1-preview 大模型

![图片](https://mp.weixin.qq.com/s/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)
1. **ReFoRCE 创新点如下：**
- **表信息压缩：**
	针对大数据 库长上下文问题，采用基于模式匹配的方法，合并相似前缀或后缀的 表，为模型减负。 减少 token 数，省成本，同时降低 LLM 幻觉 。
	- **预期答案格式限制：**
	在任务开始时生成预期答案格式，并在自我优化过程中强化，要求模型严格遵循 CSV 格式输出，明确各列属性，确保答案清晰准确。
	- **潜在有用列探索：**
	设计系统方法，从简单到复杂生成 SQL 查询，逐步理解数据库结构。使用额外的 LLM 聊天会话确定相关表和列，执行查询获取结果，并通过算法动态纠错。
	- **自我优化工作流程：**
	输入处理后的信息到模型进行自我优化，通过 执行反馈修正错误 ，达到自洽性或满足终止条件时输出最终结果。对复杂查询，利用 CTE 进行分步细化。
	- **并行化：**
	启动 多个线程同时执行整个工作流程，通过投票机制 确定最可能正确的结果，提高结果的可信度和准确性，还支持不同示例的并行处理，加速整体过程。
3. 该方案 有如下优点：
- **复杂数据集表现好：**
	在 Spider 2.0-Snow 和 Spider 2.0-Lite 数据集上，ReFoRCE 的执行准确率分别达到 31.26 和 30.35，显著超越其他方法，如 Spider-Agent 在相同数据集上得分仅约 23。
	- **多数据库适应性强：**
	能支持多种 SQL dialect，在跨数据库任务 Spider 2.0-Lite 中表现出良好的适应性，尽管提示主要针对 Snowflake dialect，但仍能有效处理其他方言的情况。
	- **复杂情况处理能力优：**
	有效处理嵌套列和复杂数据类 型，在处理复杂数据库结构和查询要求时更具优势，解决了现有方法在这些方面的不足。

**🎯总结一下：大模型时代 Text-to-SQL 特点**

随着基于 LLM 技术的发展，RAG / Agent 等方法也广泛应用于 Text-to-SQL 领域，大模型时代发展出如下特点：

- 平台化

提高兼容性，便于接入其他产品，生产环境下，不同产品 schema 定义不一样，甚至用的数据库都不一样，比如：mysql / postgressql / nosql…，就要求具有平台的兼容特点。 埋个伏笔，实际产业界会引入专门的数据产品， 数据产品 负责接入不同源的实际数据表，采用 Text-to-GraphQL / NL-to-GraphQL 来调用数据湖的数据，有兴趣可以留言评论区，安排一期专门内容。

- 插件化
- 提高扩展性，便于功能扩展，如：
- 提供 记忆功能 ，缓存用户问题，提高响应速度和优化精度；
- 提供 tools 调用功能 ，用于返回数据的后处理（LLM的数据计算能力，一直是限制产业试用的短板）；
- 提供 结果渲染 功能，生成表格，图片，报告
- 自我进化

评测 -> 反馈 -> 优化 ，形成闭环，不断自动提高准确性

- 成本控制

大模型调用按照 token 数量算钱，本地部署也需要占用算力资源，因此如何优化 prompt template，降低成本，也是必须考虑的因素

> https://spider2-sql.github.io/
>
> https://arxiv.org/abs/2502.00675

持续分享 AI 见闻和思考，包括但不限于技术、工具、应用…

您的每一个评论、点赞、转发，对我都很重要~

未完待续，关注获取最新内容

继续滑动看下一个

AI爱好者小站

向上滑动看下一个

拖拽到此处完成下载

图片将完成下载

AIX智能下载器
