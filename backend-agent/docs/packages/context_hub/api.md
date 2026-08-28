# Python Context Hub 接口文档

## 1. 当前公开函数

### `build_context(request: AnalysisTaskRequest) -> dict`

当前输出：

```json
{
  "task_id": "task-demo-001",
  "question": "Compare monthly revenue for the last 6 months",
  "dataset_ids": ["dataset-sales"]
}
```

## 2. 建议扩展输入

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "tenant_id": "tenant-demo",
  "user_id": "user-demo",
  "question": "Compare monthly revenue for the last 6 months",
  "dataset_ids": ["dataset-sales"],
  "access_context": {
    "roles": ["ANALYST"],
    "allowedDatasets": ["dataset-sales"],
    "maskedColumns": ["phone"]
  }
}
```

## 3. 建议扩展输出

```json
{
  "task_id": "task-demo-001",
  "trace_id": "trace-demo-001",
  "question": "Compare monthly revenue for the last 6 months",
  "dataset_ids": ["dataset-sales"],
  "dataset_profiles": [
    {
      "datasetId": "dataset-sales",
      "columns": ["order_month", "revenue"]
    }
  ],
  "access_context": {
    "roles": ["ANALYST"],
    "maskedColumns": ["phone"]
  }
}
```
