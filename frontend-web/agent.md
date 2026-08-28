# Frontend Agent Contract

## 1. Scope

`frontend-web` is the presentation layer of the project.

This directory is responsible for:

- login and local prototype session entry
- login and registration UI
- workbench dashboard and task state views
- dataset upload and registration UI
- dataset management and preview UI
- task creation UI
- task history and trace inspection UI
- personal profile and security notice UI
- SSE-based task progress display
- SQL review and approval UI
- chart and result rendering
- trace and warning visualization

This directory is not responsible for:

- authentication source of truth
- authorization decisions
- SQL execution
- direct model invocation
- direct Kafka, Redis, or MySQL access

## 2. Required Stack

- `Vue 3`
- `Vite`
- `TypeScript`
- `Tailwind CSS`
- `ECharts`

Rules:

- use `Vue Router` for page routing
- use Composition API and typed reactive state
- keep shared domain types and mock contracts centralized under `src/lib/`
- prefer mock-first UI contracts that can be replaced by Java backend APIs later

## 3. Module Boundaries

- `src/pages/`
  - route-level page entries
- `src/components/`
  - reusable UI components only
- `src/lib/`
  - frontend utilities, typed contracts, mock data, store, formatters, API adapters
- `src/`
  - app bootstrap, router, shared styles

Do not place backend business logic in UI components.

## 4. SSE Rules

- all streaming to the browser must use SSE exposed by `backend-java`
- do not connect directly from frontend to `backend-agent`
- do not infer final task state from local UI assumptions
- UI must treat `final_answer` and `task_failed` as terminal events
- every SSE event must be parsed as typed JSON

## 5. Security Rules

- never embed secrets in frontend code
- never place API keys in browser-visible env vars
- never trust frontend-only permission checks
- all dataset, SQL, and approval permissions must come from Java backend
- escape and sanitize server-returned rich content before rendering

## 6. Development Rules

- prefer small feature modules over very large page files
- keep UI state local unless cross-page sharing is required
- avoid hardcoding backend response shapes in multiple places
- use explicit loading, error, empty, and review-required states
- charts must render from structured config, not ad hoc string parsing
- current page set is:
  - `/login`
  - `/register`
  - `/`
  - `/datasets`
  - `/history`
  - `/profile`
- prototype login may use local mock session state, but production auth remains Java-owned

## 7. Self-Evolution Rules

This local contract must evolve whenever frontend patterns become stable.

Automatic updates are required when:

- a new major UI feature area is added
- SSE event shapes stabilize or change
- shared frontend API contracts move or are renamed
- a consistent page or feature layering emerges
- chart rendering conventions become standardized

Automatic updates are not allowed for:

- bypassing Java-owned permissions
- introducing direct frontend access to Python internals
- embedding secrets or hidden privileged behavior

## 8. Non-Goals

- no direct LLM orchestration
- no prompt storage
- no business permission engine
- no direct database adapters
