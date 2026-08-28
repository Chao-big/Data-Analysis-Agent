# Python Worker 接口与事件文档

## 1. 运行入口

1. 启动命令：`python -m worker.main`

## 2. 消费主题

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

## 3. 内部状态装配

Worker 需要将 Kafka 事件转换为 `AgentState`：

1. `task_id`
2. `trace_id`
3. `question`
4. `dataset_ids`

## 4. 回调 Java 接口

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

## 5. 中间事件输出

建议产生以下事件类型：

1. `task_started`
2. `context_built`
3. `sql_generated`
4. `query_executed`
5. `chart_ready`
6. `task_finished`
7. `task_failed`
