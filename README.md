# Agent Data Platform Monorepo

This repository contains a dual-backend data analysis agent platform:

- `frontend-web`: Next.js UI for dataset upload, task streaming, SQL review, and chart rendering
- `backend-java`: single Spring Boot monolith with isolated module packages for auth, dataset, job, audit, and gateway concerns
- `backend-agent`: FastAPI and LangGraph runtime for agent execution, tools, memory, MCP, and guardrails
- `infra`: local infrastructure bootstrap files
- `docs`: architecture and runtime contract documents

## Initial layout

- [agent.md](C:\Users\17924\Desktop\agent项目\agent.md)
- [data-analysis-agent-architecture.md](C:\Users\17924\Desktop\agent项目\docs\data-analysis-agent-architecture.md)

## Development notes

- Java owns authentication, authorization, audit, task entry, and external SSE connections.
- Python owns agent orchestration and internal execution events.
- All SQL execution is read-only by policy.

