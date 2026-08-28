# Java Audit 模块 API 文档

## 1. 模块基础信息

1. 基础路径：`/api/audit`
2. 返回包装：`ApiResponse<T>`

## 2. 查询任务审计详情

### `GET /api/audit/tasks/{taskId}`

响应体：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "task-demo-001",
    "traceId": "trace-demo-001",
    "status": "succeeded",
    "sqlText": "SELECT ...",
    "startedAt": "2026-08-27T10:00:00Z",
    "finishedAt": "2026-08-27T10:00:30Z"
  }
}
```

## 3. 查询任务事件时间线

### `GET /api/audit/tasks/{taskId}/events`

## 4. 查询用户操作记录

### `GET /api/audit/users/{userId}/records`

查询参数：

1. `page`
2. `size`
3. `from`
4. `to`

## 5. 查询高风险拦截记录

### `GET /api/audit/risk-blocks`

查询参数：

1. `datasetId`
2. `ruleCode`
3. `status`

## 6. 调试接口

### `GET /api/audit/demo`

用途：开发期返回演示审计对象。
