原创 awaha *2025年03月15日 06:01*

# 【关键词】Text2SQL；NL2SQL；开源项目选择；DB-GPT；Vanna；SuperSonic；DB-GPT-hub；LangChain；AI Agent；RAG；Fine-Tuning

这是系列文章的第三篇，之前写了 Text2SQL 的核心应用方式和业内公认的 数据集 、当下 SOTA 的 LLM-based 的实现方式和耶鲁大学 2024 年数据集 Spider 2，排行榜 TOP1 的 实现：

[Text2SQL / NL2SQL（一）数据集：自然语言与数据库的“翻译桥梁”](https://mp.weixin.qq.com/s?__biz=MzU4NjY4MzA4Ng==&mid=2247483790&idx=1&sn=a6260de4910a9495531552c7c22e20e1&scene=21#wechat_redirect)

[Text2SQL / NL2SQL（二）实现：站在 LLM 肩膀上](https://mp.weixin.qq.com/s?__biz=MzU4NjY4MzA4Ng==&mid=2247483807&idx=1&sn=6762dbb78bc259c71af491230687287c&scene=21#wechat_redirect)

本篇介绍 5 个开源项目：talk is cheap, show me the code!

面对浩繁的开源项目，如何根据需要选择适合自己的，请接着往下看。结论请直接跳到最后一章。

未完待续，下期预告评估标准和评估系统设计， 更多更新内容请关注本公众号！

**012**

**老规矩，先看效果**

下面的 demo 来自试用的 SuperSonic，将会在下面详细介绍：

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

**02**

**大模型时代 Text-to-SQL 特点**

随着基于 LLM 技术的发展，RAG / AI Agent / Fine-tuning 等方法也广泛应用于 Text-to-SQL 领域，大模型时代发展出如下特点：

- 平台化

提高兼容性，便于接入其他产品，生产环境下，不同产品 schema 定义不一样，甚至用的数据库都不一样，比如：mysql / postgressql / nosql…，就要求具有平台的兼容特点

- 插件化

提高扩展性，便于功能扩展，如：

提供 记忆功能 ，缓存用户问题，提高响应速度；

提供 tools 调用 功能，用于返回数据的后处理（LLM的数据计算能力，一直是限制产业试用的短板）；

提供 结果渲染 功能，生成表格，图片，报告，markdown

- 自我进化

评测 -> 反馈 -> 优化，形成闭环， 不 断 丰富数据集 ，提高准确性。text2sql 本质是数据产品，高质量的数据集决定了项目成功与否

- 成本控制

大模型调用按照 token 数量算钱，本地部署也需要占用算力资源，因此如何优化 prompt template，降低成本，也是必须考虑的因素

**032**

**开源项目详细介绍**

借助这些开源项目，可以方便快速构建出 demo，对方案验证很有作用。这些开源项目，大多能方便导入自己的数据库配置，而且可以本地化部署，保证数据安全；同时还能配置不同的 LLM，让系统能力与时俱进，比如可以接入当下能力很强的 claude-3.7，openai-o1，deepseek-r1…

统计时间：2025.03.14

| 项目名称 | 链接 | github  star |
| --- | --- | --- |
| DB-GPT | https://github.com/eosphoros-ai/DB-GPT | 15.5k |
| Vanna | https://github.com/vanna-ai/vanna | 14.1k |
| SuperSonic | https://github.com/tencentmusic/supersonic | 3.2k |
| DB-GPT-hub | https://github.com/eosphoros-ai/DB-GPT-Hub | 1.7k |
| LangChain | https://python.langchain.com/v0.1/docs/use\_cases/sql/ | \- |

**🎯DB-GPT & DB-GPT-hub**

https://github.com/eosphoros-ai/DB-GPT

https://github.com/eosphoros-ai/DB-GPT-Hub

https://github.com/antvis/GPT-Vis

A I Agent + RAG + Fine-tuning

设计架构图：

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

调研特点总结：

1\. 支持微调（fine-tuning）

微调支持的模型：LLaMA、LLaMA-2、BLOOM、BLOOMZ、Falcon、Baichuan、Baichuan2、InternLM、Qwen、XVERSE、ChatGLM2

支持的微调技术： LoRA / QLoRA / Pturning

2\. 有 UI 界面，如下图：用 streamlit 实现（顺带说一下，streamlit 是纯 python 的 web 神器，非常适合数据项目和 LLM 项目试用）： https://github.com/antvis/GPT-Vis

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

3. 支持的推理模型很新，Qwen2.5 / Deepseek 都支持

4\. Monitor 系统完备

5\. DB-GPT-Hub 支持微调 GQL（图数据库查询）

6\. 没有提供数据库接入，需要自己加

**🎯Vanna**

https://github.com/vanna-ai/vanna

https://vanna.ai/

https://vanna.ai/docs/postgres-openai-standard-chromadb/

AI Agent + RAG

提供 pip 包安装： https://pypi.org/project/vanna/

下面是设计图：

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

试用后特点总结：

1\. 自带 UI 界面

2\. 可以向系统输入自己的 DDL 语句 / 附加信息 / sql语句，vn.train

```python
from vanna.openai import OpenAI_Chatfrom vanna.chromadb import ChromaDB_VectorStoreclass MyVanna(ChromaDB_VectorStore, OpenAI_Chat):    def __init__(self, config=None):        ChromaDB_VectorStore.__init__(self, config=config)        OpenAI_Chat.__init__(self, config=config)
vn = MyVanna(config={'api_key': 'sk-...', 'model': 'gpt-4-...'})
```

```python
# 添加训练数据的示例# DDL 语句功能强大，因为它们指定了表名、列名、数据类型，还可能指定了关系。vn.train(ddl="""CREATE TABLE IF NOT EXISTS my-table (id INT PRIMARY KEY,name VARCHAR(100),age INT)""")# 添加关于业务术语或定义的文档说明。vn.train(documentation="Our business defines OTIF score as the percentage of orders that are delivered on time and in full")# 将 SQL 查询添加到训练数据vn.train(sql="SELECT * FROM my-table WHERE name = 'John Doe'")
```

3\. 支持的数据库更多

PostgreSQL、MySQL、PrestoDB、Apache Hive、ClickHouse、Snowflake、Oracle、Microsoft SQL Server、BigQuery、SQLite、DuckDB

**🎯 SuperSonic**

https://github.com/tencentmusic/supersonic

https://supersonicbi.github.io/

AI Agent + RAG

试用： http://117.72.46.148:9080

试用后的体会：功能多；且支持插件配置，方便能力扩展

特点：

1\. 支持中文， 这可能是目前独有的特点了

2\. 支持配置自定义数据集

3\. 支持 tools 调用：类似提供了 AI Agent 能力。具体来说
- 任意一个网页, 可以是一个看板, 也可以是一个解读报告, 召回之后可以把这个网页渲染到问答会话列表
	- 也可以为任意一个HTTP服务链接

比如：

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

4\. 支持的数据库类型：所有支持MySQL协议的数据库，如MySQL，Doris，StarRocks等；另外还有 Clickhouse，PostgreSQL，H2

5\. 支持 在数据库表上创建 抽象层 ：维度 / 度量 / 指标

https://supersonicbi.github.io/docs/headless-bi/%E6%A6%82%E5%BF%B5/

6\. 还有一个重要功能： 术语管理 ，用于指代自定义知识， 通过配置术语及其描述，就可以把私域知识传授给大模型。比如：对不同产品，近期的概念不一样，就可以在此处明确：

![图片](https://mp.weixin.qq.com/www.w3.org/2000/svg'%20xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3E%3C/title%3E%3Cg%20stroke='none'%20stroke-width='1'%20fill='none'%20fill-rule='evenodd'%20fill-opacity='0'%3E%3Cg%20transform='translate(-249.000000,%20-126.000000)'%20fill='%23FFFFFF'%3E%3Crect%20x='249'%20y='126'%20width='1'%20height='1'%3E%3C/rect%3E%3C/g%3E%3C/g%3E%3C/svg%3E)

**🎯 LangChain**

https://python.langchain.com/v0.1/docs/use\_cases/sql/

AI Agent + RAG

1\. 没有 UI 页面，需要自己写

2\. 内置 create\_sql\_query\_chain 方便调用： https://api.python.langchain.com/en/latest/chains/langchain.chains.sql\_database.query.create\_sql\_query\_chain.html

```javascript
from langchain.chains import create_sql_query_chainfrom langchain_openai import ChatOpenAI
llm = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)chain = create_sql_query_chain(llm, db)response = chain.invoke({"question": "How many employees are there"})response
```

3\. 方便使用 Langchain 自己的能力，引入 tools 等

4\. 也支持导入 CSV，类似 text2sql 一样交流： https://python.langchain.com/v0.1/docs/use\_cases/sql/csv/

**0432**

**总结**

DB-GPT 很全面，有 AI Agent、RAG、微调、UI，如果时间充足，想深入研究，用各种技术提高准确率，选它没错；如果追求插件化，且是中文场，需要很快实现带 UI 界面演示，就选择 SuperSonic；如果数据库特殊，如：snokeflake，就用 vanna；如果需要把 text2sql 作为一项附加功能集成到其他 AI 平台，建议选择 LangChain。

持续分享 AI 见闻和思考，包括但不限于技术、工具、应用…

您的每一个评论、点赞、转发，对我都很重要~

未完待续，关注获取最新内容

继续滑动看下一个

AI爱好者小站

向上滑动看下一个

拖拽到此处完成下载

图片将完成下载

AIX智能下载器
