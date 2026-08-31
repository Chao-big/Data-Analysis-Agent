# Java Job 模块 API 文档

## 1. 模块基础信息

1. 对前端基础路径：`/api/tasks`
2. 返回包装：`ApiResponse<T>`
3. 对 Python 内部调用：Java 主动请求 Python，不要求 Python 回调本模块

## 2. 创建分析任务

### `POST /api/tasks`

请求体：

```json
{
  "question": "分析过去12个月企业营收趋势，并输出区域排名与渠道占比",
  "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
  "chartPreferences": ["line", "bar", "pie"]
}
```

成功响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "taskId": "task-20260829-0001",
    "traceId": "trace-20260829-0001",
    "status": "queued",
    "question": "分析过去12个月企业营收趋势，并输出区域排名与渠道占比",
    "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
    "startedAt": "2026-08-29T18:00:00+08:00"
  }
}
```

## 3. 获取任务详情

### `GET /api/tasks/{taskId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "taskId": "task-20260829-0001",
    "traceId": "trace-20260829-0001",
    "status": "succeeded",
    "question": "分析过去12个月企业营收趋势，并输出区域排名与渠道占比",
    "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
    "relatedDatasetNames": ["enterprise_sales_monthly", "finance_revenue_fact"],
    "startedAt": "2026-08-29T18:00:00+08:00",
    "finishedAt": "2026-08-29T18:00:14+08:00",
    "duration": "14s",
    "connectionStatus": "closed",
    "chartPreferences": ["line", "bar", "pie"],
    "sqlText": "SELECT ...",
    "sqlReasoning": "按月聚合营收并补充区域与渠道维度。",
    "finalConclusion": "营收整体呈上升趋势。",
    "warnings": [],
    "failureReason": null,
    "reviewMessage": null,
    "resultKind": "normal",
    "charts": []
  }
}
```

## 4. SSE 订阅

### `GET /api/tasks/{taskId}/stream`

说明：

1. 返回 `text/event-stream`。
2. 事件源来自 Java 持久化后的 `analysis_task_event`。
3. 若任务已产生历史事件，连接建立后应优先回放历史事件，再继续推送增量事件。

事件示例：

```text
event: message
id: evt-0003
data: {"taskId":"task-20260829-0001","traceId":"trace-20260829-0001","eventType":"answer_delta","level":"info","timestamp":"2026-08-29T18:00:10+08:00","payload":{"text":"过去12个月营收整体呈上升趋势。"}}
```

## 5. Java -> Python 内部执行请求

### `POST /internal/tasks/{taskId}/run-stream`

该接口由 Python `agent_api` 提供，Java 主动调用。

请求体建议：

```json
{
  "taskId": "task-20260829-0001",
  "traceId": "trace-20260829-0001",
  "tenantId": "tenant-enterprise",
  "userId": "user-001",
  "question": "分析过去12个月企业营收趋势，并输出区域排名与渠道占比",
  "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
  "chartPreferences": ["line", "bar", "pie"],
  "datasets": [
    {
      "datasetId": "dataset-finance-mysql",
      "datasetType": "mysql",
      "schemaSummary": ["order_month", "product_line", "net_revenue", "gross_margin", "region"],
      "permissionScope": "finance.revenue.readonly"
    }
  ],
  "accessContext": {
    "allowedTables": ["finance.revenue_fact"],
    "maskedColumns": [],
    "rowLimit": 1000,
    "queryTimeoutSeconds": 30
  }
}
```

## 6. Python -> Java 流式返回契约

Java 按行消费 `application/x-ndjson`：

```json
{"seq":1,"taskId":"task-20260829-0001","traceId":"trace-20260829-0001","eventType":"task_started","level":"info","timestamp":"2026-08-29T18:00:01+08:00","payload":{"message":"任务已启动"}}
{"seq":2,"taskId":"task-20260829-0001","traceId":"trace-20260829-0001","eventType":"sql_delta","level":"info","timestamp":"2026-08-29T18:00:04+08:00","payload":{"text":"SELECT order_month,"}}
{"seq":3,"taskId":"task-20260829-0001","traceId":"trace-20260829-0001","eventType":"task_finished","level":"success","timestamp":"2026-08-29T18:00:14+08:00","payload":{"resultKind":"normal"}}
```

## 7. 调试接口

### `GET /api/jobs/demo`

保留为骨架示例接口，不代表最终任务中心契约。
