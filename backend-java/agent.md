# Backend Java Agent Contract

## 1. Scope

`backend-java` is a single Spring Boot monolith and is the authoritative business backend.

This directory is responsible for:

- authentication and authorization
- tenant and role isolation
- dataset and file metadata management
- task creation and lifecycle entry
- audit logging
- external SSE connection management
- Java-side API contracts exposed to frontend

This directory is not responsible for:

- LangGraph orchestration
- prompt engineering
- model tool selection
- MCP reasoning logic

## 2. Required Stack

- `Java 21`
- `Spring Boot 3.5.16`
- `Spring Security`
- `MySQL`
- `Redis`
- `Kafka`

Optional internal choices:

- `Spring Data JPA`
- `MyBatis-Flex`

Version rules:

- remain on the `3.5.x` Spring Boot line unless explicitly approved otherwise
- prefer stable patch upgrades within `3.5.x`
- do not adopt `4.x` automatically

## 3. Architecture Rules

This project uses a monolith, not microservices.

Code should remain isolated by module package:

- `common/`
  - shared infra and cross-cutting code only
- `modules/auth`
  - identity, roles, permissions, access context
- `modules/dataset`
  - dataset registration, file metadata, schema summary entry
- `modules/job`
  - task creation, event publishing, task status aggregation
- `modules/audit`
  - audit logs, approvals, replay metadata
- `modules/gateway`
  - external-facing API aggregation and SSE endpoints

Recommended internal layering per module:

- `controller`
- `service`
- `repository`
- `domain`
- `dto`
- `config`

## 4. Permission Rules

- Java is the source of truth for permissions
- Python agent must only receive least-privilege task context
- all dataset access must be tenant-scoped
- row, column, and dataset visibility must be enforced before task dispatch
- sensitive columns must be masked or blocked before they reach the agent

Do not delegate final authorization decisions to Python.

## 5. SSE Rules

- frontend SSE connections must terminate in Java
- Python agent may emit internal events, but Java owns browser delivery
- SSE event payloads must be stable, typed, and auditable
- disconnected clients must not corrupt task state

## 6. Development Rules

- do not collapse module boundaries into a giant `service` package
- shared code in `common/` must remain infrastructure-oriented
- business rules should live in module services, not controllers
- controllers should return typed DTOs
- event models should be explicit and versionable
- every sensitive operation should generate audit records

## 7. Security Rules

- do not hardcode secrets
- do not expose internal Python endpoints directly to browsers
- do not allow write SQL through analysis task APIs
- all outbound calls to `backend-agent` must carry trace and task identifiers

## 8. Self-Evolution Rules

This local contract must evolve whenever the Java monolith changes in a stable way.

Automatic updates are required when:

- a new Java module is added or removed
- module layering becomes more concrete
- SSE DTOs or controller conventions stabilize
- security, audit, or permission behavior becomes stricter
- the Java framework baseline changes within approved boundaries

Automatic updates are not allowed for:

- changing away from monolith architecture
- weakening authorization boundaries
- switching away from Spring Boot `3.5.x` without explicit approval

## 9. Non-Goals

- no embedded LLM prompt templates here
- no business logic hidden in SSE handlers
- no direct frontend trust for approval or permission checks
