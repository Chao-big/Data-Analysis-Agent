# Java Gateway 模块 API 文档

## 1. 模块基础信息

1. 基础路径：`/api/gateway`
2. 返回包装：`ApiResponse<T>`

## 2. 健康检查

### `GET /api/gateway/health`

当前骨架响应：

```json
{
  "success": true,
  "message": "ok",
  "data": "backend-java-monolith"
}
```

## 3. 工作台初始化数据

### `GET /api/gateway/workbench/bootstrap`

响应体建议：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "user": {
      "userId": "user-001",
      "roles": ["ANALYST"]
    },
    "datasets": [
      {"datasetId": "dataset-sales", "datasetName": "sales"}
    ],
    "recentTasks": []
  }
}
```

## 4. 任务 SSE 订阅

### `GET /api/gateway/tasks/{taskId}/stream`

说明：

1. Java 对前端保持 SSE 长连接
2. 事件来源可来自 MySQL 状态轮询、Redis 热点状态或 Kafka 状态回传
3. 事件格式与 `frontend-web/lib/types.ts` 的 `StreamEvent` 对齐
