# Python Guardrails 接口文档

## 1. 当前公开函数

### `validate_sql(sql: str) -> bool`

当前规则：

1. SQL 大写化
2. 只要包含禁止关键字之一即返回 `false`

## 2. 当前禁止关键字

1. `INSERT`
2. `UPDATE`
3. `DELETE`
4. `DROP`
5. `ALTER`
6. `TRUNCATE`
7. `CREATE`

## 3. 建议扩展接口

### `validate_sql(sql: str, allowed_tables: list[str], masked_columns: list[str]) -> dict`

返回示例：

```json
{
  "passed": false,
  "ruleCode": "SQL_FORBIDDEN_TOKEN",
  "message": "SQL contains forbidden token: DROP",
  "riskLevel": "high"
}
```

## 4. 风险输出字段建议

1. `passed`
2. `ruleCode`
3. `message`
4. `riskLevel`
5. `blockedSql`
