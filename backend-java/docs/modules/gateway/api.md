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

## 4. 说明

初版不在 `gateway` 模块暴露任务 SSE。

任务相关接口统一由 `job` 模块提供：

1. `POST /api/tasks`
2. `GET /api/tasks/{id}`
3. `GET /api/tasks/{id}/stream`
