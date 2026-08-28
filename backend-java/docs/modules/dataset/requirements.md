# Java Dataset 模块需求

## 1. 模块定位

`dataset` 模块负责统一数据集入口，管理文件型和数据库型数据集的注册、元数据、Schema 摘要、预览和授权关系。

## 2. 当前代码现状

当前仅存在 `GET /api/datasets/demo` 骨架接口。MVP 需要把数据集从演示对象扩展为真实业务实体。

## 3. 核心职责

1. 文件上传
2. 数据集注册
3. MySQL 数据源接入
4. Schema 摘要提取
5. 样例数据预览
6. 数据集列表和详情查询
7. 数据集授权关系维护

## 4. MVP 功能需求

1. 支持 `CSV / Excel` 上传
2. 支持 `MySQL` 只读数据源注册
3. 支持生成 `schema_summary`
4. 支持预览前 20 行样例数据
5. 支持按登录用户返回已授权数据集
6. 支持数据集状态管理：`REGISTERING`、`REGISTERED`、`FAILED`

## 5. 数据字段要求

1. `datasetId`
2. `datasetName`
3. `sourceType`
4. `sourceLocation`
5. `schemaSummary`
6. `status`
7. `tenantId`
8. `ownerUserId`

## 6. 边界约束

1. 不直接执行分析 SQL
2. 不直接创建分析任务
3. 只允许注册只读 MySQL 连接

## 7. 验收标准

1. 文件可上传并完成注册
2. MySQL 数据集可配置并完成连通性校验
3. 已授权用户可查看数据集摘要和预览
4. 未授权用户无法访问数据集详情
