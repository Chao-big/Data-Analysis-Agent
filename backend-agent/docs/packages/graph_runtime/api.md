# Python Graph Runtime 接口文档

## 1. 公开函数

### `build_graph()`

作用：构建并编译 LangGraph 工作流。

当前返回：

1. `workflow.compile()` 结果

## 2. 状态模型

当前 `AgentState`：

```python
class AgentState(TypedDict, total=False):
    task_id: str
    trace_id: str
    question: str
    dataset_ids: list[str]
    plan: list[str]
    sql: list[str]
    warnings: list[str]
    final_answer: str
```

## 3. 节点输入输出契约

### `load_task_context(state) -> state`

输出要求：

1. 写入任务基础信息
2. 写入上下文摘要

### `generate_sql(state) -> state`

输出要求：

1. 写入 SQL 文本
2. 写入 SQL 生成理由

### `build_chart(state) -> state`

输出要求：

1. 写入图表配置对象

### `write_answer(state) -> state`

输出要求：

1. 写入最终结论
2. 写入警告列表

## 4. 建议事件输出格式

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "event_type": "sql_generated",
  "timestamp": "2026-08-27T10:00:03Z",
  "message": "Generated read-only SQL"
}
```
