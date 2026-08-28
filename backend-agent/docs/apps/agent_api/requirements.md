# Python Agent API 模块需求

## 1. 模块定位

`apps/agent_api` 是 Python Agent 对内服务入口，对应当前 `backend-agent/apps/agent_api/main.py`。它负责接收 Java 主后端下发的分析任务、暴露健康检查并提供任务状态查询。

## 2. 当前代码现状

当前已实现：

1. `GET /health`
2. `POST /internal/tasks`

当前仍缺少真实任务持久化、状态查询和取消能力。

## 3. 核心职责

1. 接收 Java 发起的内部任务请求
2. 校验任务请求结构
3. 将任务交给内部执行链路
4. 提供健康检查
5. 提供内部任务状态查询

## 4. MVP 功能需求

1. 支持接收 `AnalysisTaskRequest`
2. 支持返回 `task_id`、`trace_id` 和受理状态
3. 支持按 `task_id` 查询任务当前状态
4. 支持统一错误返回和参数校验
5. 支持为日志和事件透传 `trace_id`

## 5. 输入模型要求

基于当前 `shared_models.task.AnalysisTaskRequest`，最小字段为：

1. `task_id`
2. `trace_id`
3. `tenant_id`
4. `user_id`
5. `question`
6. `dataset_ids`

## 6. 边界约束

1. 不负责最终 UI 展示
2. 不负责业务权限判定
3. 不直接对前端开放

## 7. 验收标准

1. Java 能成功调用内部任务接入接口
2. 参数缺失会被明确校验拒绝
3. 内部状态查询能返回任务是否已进入执行链路
