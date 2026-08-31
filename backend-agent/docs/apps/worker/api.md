# Python Worker 接口与事件文档

## 1. 运行入口

1. 启动命令：`python -m worker.main`

## 2. 当前阶段说明

初版主链不经过 Worker。

当前推荐主链为：

1. Java -> `agent_api` HTTP 流式请求
2. `agent_api` -> LangGraph
3. `agent_api` -> Java NDJSON 事件流

Worker 文档保留给第二阶段 Kafka 化演进。

## 3. 消费主题

### `analysis.task.created`

事件体：

```json
{
  "taskId": "task-demo-001",
  "tenantId": "tenant-demo",
  "userId": "user-demo",
  "traceId": "trace-demo-001",
  "question": "Compare monthly revenue for the last 6 months"
}
```

## 4. 内部状态装配

Worker 需要将 Kafka 事件转换为 `AgentState`：

1. `task_id`
2. `trace_id`
3. `question`
4. `dataset_ids`

## 5. 回调 Java 接口

### `POST /internal/agent/tasks/{taskId}/callback`

请求体建议：

```json
{
  "traceId": "trace-demo-001",
  "status": "succeeded",
  "sqlText": "SELECT ...",
  "finalAnswer": "最近6个月营收整体上升。",
  "warnings": [],
  "chartOption": {},
  "finishedAt": "2026-08-27T10:00:30Z"
}
```

## 6. 中间事件输出

建议产生以下事件类型：

1. `task_started`
2. `context_built`
3. `sql_delta`
4. `sql_ready`
5. `query_executed`
6. `answer_delta`
7. `chart_ready`
8. `task_finished`
9. `task_failed`
