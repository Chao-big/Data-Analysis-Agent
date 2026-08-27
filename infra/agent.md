# Infra Agent Contract

## 1. Scope

`infra` stores environment and infrastructure configuration for local development and future deployment.

This directory is responsible for:

- local dependency bootstrap
- container and deployment manifests
- database initialization support
- Redis, Kafka, and observability config placeholders

This directory is not responsible for:

- business logic
- frontend code
- Java module implementation
- Python agent implementation

## 2. Subdirectory Responsibilities

- `docker/`
  - local `docker-compose` and dependency startup
- `mysql/`
  - schema bootstrap, migration notes, seed entrypoints
- `redis/`
  - caching and runtime notes
- `kafka/`
  - topic definitions and consumer-group notes
- `k8s/`
  - deployment manifests for later stages
- `observability/`
  - Prometheus, Grafana, OpenTelemetry related configs

## 3. Development Rules

- keep infra config environment-specific and explicit
- prefer checked-in templates over undocumented manual steps
- local config should be runnable with minimal edits
- production-only settings should not be mixed into local defaults

## 4. Security Rules

- never commit real passwords, tokens, or private certificates
- use placeholders or env-based injection
- do not expose databases or brokers publicly by default
- local ports should be documented and intentional

## 5. Stack Rules

This project currently assumes:

- `MySQL`
- `Redis`
- `Kafka`
- optional observability stack

If a new dependency is introduced, document:

- why it is needed
- which service uses it
- default ports
- startup order

## 6. Self-Evolution Rules

This local contract must evolve whenever infra assumptions become real operational standards.

Automatic updates are required when:

- a new infrastructure dependency is added
- startup order changes
- docker, k8s, or observability conventions become standardized
- ports, topics, or bootstrap steps become stable repository conventions

Automatic updates are not allowed for:

- committing real secrets
- silently broadening network exposure
- introducing production-destructive defaults

## 7. Non-Goals

- no business DTOs
- no application-layer services
- no prompt files
