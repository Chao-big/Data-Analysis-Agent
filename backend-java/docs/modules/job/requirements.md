# Java Job 模块需求

## 1. 模块定位

`job` 模块负责分析任务生命周期管理，是 Java 业务入口与 Python Agent Runtime 之间的异步桥梁。

## 2. 当前代码现状

当前仅存在 `GET /api/jobs/demo` 骨架接口，并复用了 `AnalysisTaskEvent` 作为演示返回。MVP 需要扩展为真实任务中心。

## 3. 核心职责

1. 创建分析任务
2. 生成 `taskId` 和 `traceId`
3. 持久化任务状态
4. 向 Kafka 投递任务消息
5. 接收 Python 回调结果
6. 向前端提供任务详情查询

## 4. MVP 功能需求

1. 支持按问题和数据集创建任务
2. 支持任务状态流转
3. 支持记录任务开始和完成时间
4. 支持接收 Agent 最终结果和失败信息
5. 支持保存最终 SQL、图表结果和结论摘要

## 5. 状态流转

1. `queued`
2. `running`
3. `needs_review`
4. `succeeded`
5. `failed`

## 6. 事件要求

Kafka 投递消息至少包含：

1. `taskId`
2. `tenantId`
3. `userId`
4. `traceId`
5. `question`

## 7. 验收标准

1. 成功创建任务后 MySQL 中有任务记录
2. 任务可成功投递 Kafka
3. Python 回调后任务状态正确更新
4. 前端可查询最终任务结果
