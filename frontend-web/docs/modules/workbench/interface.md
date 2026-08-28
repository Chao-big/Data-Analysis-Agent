# 分析工作台首页接口文档

## 1. 文档目的

本文档定义首页工作台依赖的前后端接口契约，覆盖页面初始化、数据集查询、任务创建、任务详情查询、SSE 订阅和结果结构。

首页只与 `backend-java` 交互，不直接连接 `backend-agent`。

## 2. 接口范围

首页依赖的核心接口：

1. `GET /api/datasets`
2. `GET /api/datasets/{id}`
3. `POST /api/datasets/upload`
4. `POST /api/datasets/mysql/register`
5. `POST /api/tasks`
6. `GET /api/tasks/{id}`
7. `GET /api/tasks/{id}/stream`

说明：

1. `upload` 和 `mysql/register` 不一定在首页主区域直接使用，但首页的数据集接入入口会依赖它们
2. `POST /internal/agent/tasks/{id}/callback` 属于后端内部回调，不属于前端调用范围

## 3. 通用约定

### 3.1 认证

所有首页接口均要求用户已登录，由 Java 后端识别登录态。

### 3.2 返回原则

建议所有接口统一返回：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

失败时：

```json
{
  "code": "TASK_CREATE_FAILED",
  "message": "任务创建失败",
  "data": null
}
```

### 3.3 时间字段

所有时间字段建议使用 ISO 8601 字符串或后端统一的标准时间字符串，前端只负责展示，不自行推导服务端事实时间。

## 4. 数据集接口

### 4.1 获取当前用户可用数据集列表

`GET /api/datasets`

#### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | string | 否 | 按数据集名称搜索 |
| `datasetType` | string | 否 | `csv` / `excel` / `mysql` |
| `page` | number | 否 | 页码，MVP 可选 |
| `pageSize` | number | 否 | 分页大小，MVP 可选 |

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [
      {
        "datasetId": "dataset-finance-mysql",
        "datasetName": "finance_revenue_fact",
        "datasetType": "mysql",
        "sourceLocation": "mysql://readonly@10.12.3.18:3306/finance.revenue_fact",
        "schemaSummary": ["order_month", "product_line", "net_revenue", "gross_margin", "region"],
        "owner": "finance_admin",
        "tenantId": "tenant-enterprise",
        "createdAt": "2026-08-26T10:05:00+08:00",
        "permissionScope": "finance.revenue.readonly",
        "rowCount": 6820,
        "description": "财务营收事实表"
      }
    ],
    "total": 1
  }
}
```

#### 前端用途

1. 首页左侧快捷数据集列表
2. 提交区数据集选择器
3. 数据集管理入口的基础数据源

### 4.2 获取数据集详情

`GET /api/datasets/{id}`

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | string | 是 | 数据集 ID |

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "datasetId": "dataset-finance-mysql",
    "datasetName": "finance_revenue_fact",
    "datasetType": "mysql",
    "sourceLocation": "mysql://readonly@10.12.3.18:3306/finance.revenue_fact",
    "schemaSummary": ["order_month", "product_line", "net_revenue", "gross_margin", "region"],
    "sampleRows": [
      {
        "order_month": "2026-06",
        "product_line": "企业服务",
        "net_revenue": 402100,
        "gross_margin": 0.38,
        "region": "华东"
      }
    ],
    "owner": "finance_admin",
    "tenantId": "tenant-enterprise",
    "createdAt": "2026-08-26T10:05:00+08:00",
    "permissionScope": "finance.revenue.readonly",
    "rowCount": 6820,
    "description": "财务营收事实表",
    "status": "active"
  }
}
```

#### 前端用途

1. 展示 Schema 摘要
2. 展示样例数据预览
3. 辅助用户判断是否适合作为分析输入

### 4.3 上传文件型数据集

`POST /api/datasets/upload`

#### Content-Type

`multipart/form-data`

#### 表单字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 是 | `csv` 或 `excel` 文件 |
| `datasetName` | string | 是 | 数据集名称 |
| `description` | string | 否 | 数据集描述 |

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "datasetId": "dataset-enterprise-sales",
    "datasetName": "enterprise_sales_monthly",
    "datasetType": "csv",
    "status": "active"
  }
}
```

#### 前端用途

1. 首页“数据集接入”入口
2. 上传成功后刷新数据集列表并可直接选中

### 4.4 注册 MySQL 数据集

`POST /api/datasets/mysql/register`

#### 请求体

```json
{
  "datasetName": "finance_revenue_fact",
  "host": "10.12.3.18",
  "port": 3306,
  "database": "finance",
  "username": "readonly",
  "tables": ["revenue_fact"]
}
```

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "datasetId": "dataset-finance-mysql",
    "datasetName": "finance_revenue_fact",
    "datasetType": "mysql",
    "status": "active"
  }
}
```

#### 前端用途

1. 首页“注册 MySQL 数据集”入口
2. 注册成功后刷新数据集列表

## 5. 任务接口

### 5.1 创建分析任务

`POST /api/tasks`

#### 请求体

```json
{
  "question": "分析过去12个月各月营收趋势，并输出区域排名与渠道占比",
  "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
  "chartPreferences": ["line", "bar", "pie"]
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `question` | string | 是 | 自然语言分析问题 |
| `datasetIds` | string[] | 是 | 至少一个已授权数据集 |
| `chartPreferences` | string[] | 否 | `auto` / `line` / `bar` / `pie` |

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "taskId": "task-20260827-0101",
    "traceId": "trace-20260827-0101",
    "status": "queued",
    "question": "分析过去12个月各月营收趋势，并输出区域排名与渠道占比",
    "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
    "startedAt": "2026-08-27T10:02:12+08:00"
  }
}
```

#### 前端处理要求

1. 创建成功后将该任务置为当前任务
2. 立即发起详情轮询或 SSE 订阅
3. 本地进入 `queued` 或 `running` 视觉状态

### 5.2 获取任务详情

`GET /api/tasks/{id}`

#### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | string | 是 | 任务 ID |

#### 响应体

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "taskId": "task-20260827-0101",
    "traceId": "trace-20260827-0101",
    "status": "succeeded",
    "question": "分析过去12个月各月营收趋势，并输出区域排名与渠道占比",
    "datasetIds": ["dataset-enterprise-sales", "dataset-finance-mysql"],
    "relatedDatasetNames": ["enterprise_sales_monthly", "finance_revenue_fact"],
    "startedAt": "2026-08-27T10:02:12+08:00",
    "finishedAt": "2026-08-27T10:02:28+08:00",
    "duration": "16s",
    "connectionStatus": "closed",
    "chartPreferences": ["line", "bar", "pie"],
    "sqlText": "SELECT order_month, SUM(net_revenue) AS total_revenue FROM finance.revenue_fact GROUP BY order_month ORDER BY order_month ASC;",
    "sqlReasoning": "按月聚合营收，用于趋势分析，并补充区域和渠道维度。",
    "finalConclusion": "营收整体呈上升趋势，华东和华南贡献最高。",
    "warnings": ["结果基于汇总数据，不包含退款明细。"],
    "failureReason": null,
    "reviewMessage": null,
    "resultKind": "normal",
    "charts": [
      {
        "id": "chart-line-revenue",
        "title": "月度营收趋势",
        "description": "展示最近12个月营收变化",
        "type": "line",
        "option": {}
      }
    ]
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `status` | string | `queued` / `running` / `needs_review` / `succeeded` / `failed` |
| `connectionStatus` | string | 前端展示用状态，可由 SSE 生命周期映射 |
| `resultKind` | string | `normal` / `empty` / `blocked` / `review` |
| `warnings` | string[] | 风险提示或结果说明 |
| `charts` | array | 图表配置列表 |

#### 前端用途

1. 渲染结果区
2. 渲染 SQL 区
3. 渲染审计信息区
4. 切换历史任务后恢复详情

## 6. SSE 接口

### 6.1 订阅任务事件流

`GET /api/tasks/{id}/stream`

#### Headers

建议：

| Header | 说明 |
|---|---|
| `Accept: text/event-stream` | SSE 必需 |
| `Last-Event-ID` | 可选，用于断线续传 |

#### 事件格式

服务端返回标准 SSE：

```text
event: message
id: evt-001
data: {"eventType":"task_started","taskId":"task-20260827-0101","traceId":"trace-20260827-0101","timestamp":"2026-08-27T10:02:12+08:00","message":"任务已创建，开始准备分析上下文","level":"info"}
```

#### `data` JSON 结构

```json
{
  "eventType": "task_started",
  "taskId": "task-20260827-0101",
  "traceId": "trace-20260827-0101",
  "timestamp": "2026-08-27T10:02:12+08:00",
  "message": "任务已创建，开始准备分析上下文",
  "level": "info"
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `eventType` | string | 事件类型 |
| `taskId` | string | 任务 ID |
| `traceId` | string | 追踪 ID |
| `timestamp` | string | 事件时间 |
| `message` | string | 展示文案 |
| `level` | string | `info` / `warning` / `success` / `error` |

### 6.2 支持的事件类型

| 事件 | 说明 | 是否终态 |
|---|---|---|
| `task_started` | 任务已创建 | 否 |
| `context_built` | 权限上下文和 Schema 已准备 | 否 |
| `sql_generated` | SQL 已生成或被拦截 | 否 |
| `query_executed` | 查询执行完成 | 否 |
| `chart_ready` | 图表配置已生成 | 否 |
| `human_review_required` | 命中高风险规则，需要人工确认说明 | 否 |
| `task_finished` | 任务成功完成 | 是 |
| `task_failed` | 任务失败或被阻断 | 是 |

#### 前端处理规则

1. `task_finished` 和 `task_failed` 视为终态事件
2. 前端不可因为本地超时自行把任务改为失败
3. 收到终态事件后，应主动刷新一次任务详情，确保结果完整

## 7. 首页前端建议类型定义

建议前端统一使用以下类型：

```ts
export type TaskStatus = "queued" | "running" | "needs_review" | "succeeded" | "failed";
export type ConnectionStatus = "idle" | "connecting" | "open" | "closed" | "error";
export type EventLevel = "info" | "warning" | "success" | "error";
export type StreamEventType =
  | "task_started"
  | "context_built"
  | "sql_generated"
  | "query_executed"
  | "chart_ready"
  | "task_finished"
  | "task_failed"
  | "human_review_required";
```

## 8. 错误码建议

首页重点关注以下错误场景：

| 错误码 | 场景 | 前端处理 |
|---|---|---|
| `DATASET_NOT_FOUND` | 数据集不存在 | 提示刷新数据集列表 |
| `DATASET_FORBIDDEN` | 数据集未授权 | 提示无权限访问 |
| `TASK_CREATE_FAILED` | 任务创建失败 | 显示失败消息并允许重试 |
| `TASK_NOT_FOUND` | 任务不存在 | 提示任务已失效 |
| `SQL_BLOCKED` | SQL 被风控拦截 | 展示拦截原因 |
| `HUMAN_REVIEW_REQUIRED` | 高风险任务需人工确认 | 展示审核态 |
| `SSE_STREAM_CLOSED` | SSE 连接关闭 | 显示连接关闭状态 |

## 9. 联调顺序建议

建议按以下顺序联调首页：

1. `GET /api/datasets`
2. `POST /api/tasks`
3. `GET /api/tasks/{id}/stream`
4. `GET /api/tasks/{id}`
5. `POST /api/datasets/upload`
6. `POST /api/datasets/mysql/register`

这样可以先打通首页核心链路，再补数据接入入口。
