# Python Tool Chart 接口文档

## 1. 当前公开函数

### `build_line_chart(title: str) -> dict`

当前返回示例：

```json
{
  "title": {"text": "Monthly Revenue"},
  "xAxis": {"type": "category", "data": ["Jan", "Feb", "Mar"]},
  "yAxis": {"type": "value"},
  "series": [{"type": "line", "data": [120, 132, 101]}]
}
```

## 2. 建议扩展函数

### `build_chart(question: str, rows: list, chart_type: str | None = None) -> dict`

参数说明：

1. `question`：用户问题
2. `rows`：查询结果
3. `chart_type`：可选图表类型

## 3. 错误约定

1. 当 `rows` 为空时返回空态图表或 `warnings`
2. 当数据维度不适合图表时返回 `chartType = none`
