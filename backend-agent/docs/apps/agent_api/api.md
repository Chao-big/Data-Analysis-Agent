# Python Agent API 接口文档

## 1. 基础信息

1. 服务名称：`agent-api`
2. 基础路径：`/`
3. 框架：`FastAPI`

## 2. 健康检查

### `GET /health`

当前响应：

```json
{
  "status": "ok",
  "service": "agent-api"
}
```

## 3. 创建内部任务

### `POST /internal/tasks`

请求体：

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "tenant_id": "tenant-demo",
  "user_id": "user-demo",
  "question": "Compare monthly revenue for the last 6 months",
  "dataset_ids": ["dataset-sales"]
}
```

当前骨架响应：

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "status": "accepted"
}
```

## 4. 查询任务状态

### `GET /internal/tasks/{taskId}`

建议响应：

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "status": "running",
  "current_step": "generate_sql"
}
```

## 5. 错误约定

1. `400`：请求结构错误
2. `404`：任务不存在
3. `422`：Pydantic 参数校验失败
4. `500`：内部执行链路异常
