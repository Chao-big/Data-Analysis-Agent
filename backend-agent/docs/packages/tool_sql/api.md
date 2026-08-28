# Python Tool SQL 接口文档

## 1. 公开函数

### `build_read_only_sql(question: str) -> SqlPlan`

当前返回模型：

```python
class SqlPlan(BaseModel):
    sql: str
    reasoning: str
```

当前骨架返回示例：

```json
{
  "sql": "SELECT order_month, SUM(revenue) AS total_revenue FROM sales GROUP BY order_month ORDER BY order_month;",
  "reasoning": "Scaffold SQL for question: Compare monthly revenue for the last 6 months"
}
```

## 2. 建议扩展函数

### `build_read_only_sql(question: str, schema: dict, access_context: dict) -> SqlPlan`

### `run_read_only_sql(plan: SqlPlan, datasource: dict) -> dict`

输出示例：

```json
{
  "columns": ["order_month", "total_revenue"],
  "rows": [
    ["2026-01", 120000],
    ["2026-02", 132000]
  ],
  "rowCount": 2
}
```

## 3. 错误返回建议

```json
{
  "errorCode": "SQL_FORBIDDEN_TOKEN",
  "message": "SQL contains forbidden token: DROP"
}
```
