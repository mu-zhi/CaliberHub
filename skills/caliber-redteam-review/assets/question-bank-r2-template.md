# 口径文档质询题库 R2 模板（240题）

> 用于生成 `6×4×10` 网格题库，字段与主表一致。

| question_id | dimension | source_doc | scenario_anchor | question | answer_direction | required_evidence | fail_condition | severity_default | owner_role |
|---|---|---|---|---|---|---|---|---|---|
| Q0001 | 语义歧义 | 03-其他文档/09-... | 业务定义（L6-L9） | （问题） | （回答方向） | 03-其他文档/09-...:L6-L9 \| excerpt:"..." \|\| contrast:03-其他文档/09-...:L93-L96 \| excerpt:"..." | （不通过条件） | P0 | 业务架构角色 |
