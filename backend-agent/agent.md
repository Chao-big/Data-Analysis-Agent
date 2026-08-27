# Backend Agent Contract

## 1. Scope

`backend-agent` is the Python runtime for the analysis agent.

This directory is responsible for:

- LangGraph orchestration
- tool registration and invocation
- SQL planning and guardrails
- Python post-processing
- chart generation
- memory management
- MCP integration
- context engineering
- prompt engineering
- evals and regression checks

This directory is not responsible for:

- primary authentication
- primary authorization
- browser-facing SSE ownership
- direct user identity trust without Java context

## 2. Required Stack

- `Python 3.12`
- `FastAPI`
- `LangGraph`
- `Pydantic`
- `SQLAlchemy`
- `Redis`

Model-facing integrations should prefer:

- `OpenAI Responses API`
- structured outputs
- typed tool schemas

## 3. Package Boundaries

- `apps/agent_api`
  - internal HTTP entrypoints only
- `apps/worker`
  - Kafka consumers and runtime workers
- `packages/graph_runtime`
  - graph state, nodes, transitions, resume logic
- `packages/tool_registry`
  - tool catalog and policy gates
- `packages/tool_sql`
  - schema inspection, SQL planning, validation, execution
- `packages/tool_python`
  - analysis and transformation logic
- `packages/tool_chart`
  - chart config generation
- `packages/memory`
  - short-term and long-term memory persistence
- `packages/mcp_hub`
  - MCP discovery and invocation
- `packages/context_hub`
  - context assembly and trimming
- `packages/prompt_hub`
  - prompt templates and version control
- `packages/guardrails`
  - safety and structural validation
- `packages/evals`
  - offline evaluation and regression checks
- `packages/shared_models`
  - typed models shared across apps and packages
- `packages/observability`
  - runtime events and metrics

Do not merge these packages into one generic utils directory.

## 4. Permission Rules

- trust only the access context produced by Java
- never expand dataset scope on the agent side
- never execute write SQL
- never call unapproved MCP servers
- do not persist memory outside tenant and user boundaries

## 5. Prompt and Context Rules

- prompts must be versioned
- context must be compact and permission-scoped
- prompt templates must live outside core business logic
- every tool-facing prompt must define a structured output target
- unsupported or unsafe requests must fail explicitly

## 6. Tooling Rules

- every tool must declare input and output schema
- every tool must have timeout and retry policy
- every tool result must be serializable
- SQL tools must enforce read-only restrictions
- chart tools must output frontend-consumable config

## 7. Streaming Rules

- Python may emit internal progress events
- event payloads must be typed and stable
- final browser-facing SSE formatting belongs to Java
- progress events must include `task_id` and `trace_id`

## 8. Evaluation Rules

- any change to prompts, tool schemas, or guardrails should be regression-tested
- track SQL correctness, failure rate, latency, and review-required rate
- keep eval data separate from production runtime code

## 9. Self-Evolution Rules

This local contract must evolve whenever the Python runtime changes in a stable way.

Automatic updates are required when:

- a new agent package becomes a standard part of the runtime
- prompt, context, memory, or MCP workflows are formalized
- tool contracts or structured outputs change
- new evaluation or guardrail requirements become standard
- internal event contracts change

Automatic updates are not allowed for:

- weakening SQL safety rules
- broadening access beyond Java-issued context
- replacing the Python runtime stack without explicit approval

## 10. Non-Goals

- no direct frontend rendering
- no root authorization logic
- no unsafe shell execution without explicit sandbox policy
