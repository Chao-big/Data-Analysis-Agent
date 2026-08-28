# Java Job 模块 API 文档

## 1. 模块基础信息

1. 基础路径：`/api/jobs`
2. 返回包装：`ApiResponse<T>`

## 2. 创建分析任务

### `POST /api/jobs`

请求体：

```json
{
  "question": "比较最近6个月营收趋势",
  "datasetIds": ["dataset-sales"]
}
```

成功响应：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "task-demo-001",
    "traceId": "trace-demo-001",
    "status": "queued"
  }
}
```

## 3. 获取任务详情

### `GET /api/jobs/{taskId}`

响应体：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "task-demo-001",
    "traceId": "trace-demo-001",
    "status": "succeeded",
    "question": "比较最近6个月营收趋势",
    "sqlText": "SELECT ...",
    "finalAnswer": "最近6个月营收整体上升。",
    "warnings": [],
    "chartOption": {}
  }
}
```

## 4. 任务列表

### `GET /api/jobs`

查询参数：

1. `status`
2. `datasetId`
3. `page`
4. `size`

## 5. Agent 回调接口

### `POST /internal/agent/tasks/{taskId}/callback`

请求体：

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

## 6. Kafka 任务事件契约

主题建议：`analysis.task.created`

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

## 7. 调试接口

### `GET /api/jobs/demo`

用途：开发期返回 `AnalysisTaskEvent` 示例。
