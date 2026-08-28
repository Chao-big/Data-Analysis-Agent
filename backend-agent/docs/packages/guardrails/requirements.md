# Python Guardrails 模块需求

## 1. 模块定位

`guardrails` 负责在 Agent 执行过程中实施安全和稳定性约束，首要目标是阻止危险 SQL 和越权操作。

## 2. 当前代码现状

当前 `validate_sql(sql)` 通过关键字黑名单过滤：

1. `INSERT`
2. `UPDATE`
3. `DELETE`
4. `DROP`
5. `ALTER`
6. `TRUNCATE`
7. `CREATE`

这只是首层保护，MVP 仍需补足访问范围、结果规模和多语句校验。

## 3. 核心职责

1. SQL 只读校验
2. 未授权访问拦截
3. 多语句拦截
4. 结果规模约束
5. 高风险任务标记

## 4. MVP 功能需求

1. 支持 forbidden token 校验
2. 支持只允许 `SELECT`
3. 支持表白名单校验
4. 支持敏感字段命中告警
5. 支持超过阈值时挂起或失败

## 5. 验收标准

1. 非法 SQL 能被拦截
2. 风险规则命中后能返回明确原因
3. 风险输出可被 `audit` 模块记录
