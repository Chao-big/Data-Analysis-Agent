# Python Graph Runtime 模块需求

## 1. 模块定位

`graph_runtime` 是 Agent 的执行编排核心，负责维护状态机、节点顺序和最终输出。

## 2. 当前代码现状

当前 `build_graph()` 仅包含：

1. `load_context`
2. `write_answer`

这只能证明图能跑通，还不能支撑真实分析任务。

## 3. 核心职责

1. 定义 `AgentState`
2. 定义最小分析节点图
3. 维护节点间状态传递
4. 统一处理中断、失败和恢复

## 4. MVP 功能需求

1. 节点至少包括：
   `load_task_context`、`schema_profile`、`build_context`、`plan_analysis`、`generate_sql`、`sql_guard`、`run_query`、`python_analysis`、`build_chart`、`write_answer`
2. 节点执行应按确定顺序推进
3. SQL 被拦截时必须进入修复或失败分支
4. 每个关键节点应产出事件
5. 最终状态应包含答案、SQL、警告和图表

## 5. 状态字段要求

1. `task_id`
2. `trace_id`
3. `question`
4. `dataset_ids`
5. `plan`
6. `sql`
7. `warnings`
8. `final_answer`

## 6. 验收标准

1. Graph 能消费真实任务上下文
2. Graph 能输出最终答案
3. Graph 能在失败时返回明确节点错误
