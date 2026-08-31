# Python Graph Runtime 接口文档

## 1. 公开函数

### `build_graph()`

作用：构建并编译 LangGraph 工作流。

当前返回：

1. `workflow.compile()` 结果

## 2. 状态模型

当前 `AgentState` 建议扩展为：

```python
class AgentState(TypedDict, total=False):
    task_id: str
    trace_id: str
    question: str
    dataset_ids: list[str]
    plan: list[str]
    sql: str
    warnings: list[str]
    final_answer: str
    event_seq: int
    event_sink: Callable[[dict], None]
```

## 3. 节点输入输出契约

### `load_task_context(state) -> state`

输出要求：

1. 写入任务基础信息
2. 写入上下文摘要
3. 发出 `task_started`

### `generate_sql(state) -> state`

输出要求：

1. 写入 SQL 文本
2. 写入 SQL 生成理由
3. 在生成过程中可多次触发 `sql_delta`
4. 完成后触发 `sql_ready`

### `build_chart(state) -> state`

输出要求：

1. 写入图表配置对象
2. 触发 `chart_ready`

### `write_answer(state) -> state`

输出要求：

1. 写入最终结论
2. 写入警告列表
3. 在生成过程中可多次触发 `answer_delta`

## 4. 建议事件输出格式

```json
{
  "seq": 1,
  "taskId": "task-demo-001",
  "traceId": "trace-demo-001",
  "eventType": "sql_ready",
  "level": "success",
  "timestamp": "2026-08-27T10:00:03Z",
  "payload": {
    "sql": "SELECT ...",
    "reasoning": "Generated read-only SQL"
  }
}
```
