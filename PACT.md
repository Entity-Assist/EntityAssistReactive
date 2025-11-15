---
version: 2.0
date: 2025-11-15
title: Entity Assist Reactive Collaboration Pact
project: EntityAssistReactive
authors: [Entity Assist Maintainers, Contributors, Codex]
---

# 🤝 Entity Assist Reactive Pact (v2)
*(Human × Codex — Documentation-First Edition)*

## 1. Purpose

EntityAssistReactive is a CRTP-first persistence library that injects fluent query builders into GuicedEE services on Vert.x 5 and Hibernate Reactive 7, delegating persistence to whichever Vert.x reactive database driver a host selects (PostgreSQL via vertx-pg-client by default). This pact captures the shared culture required to keep that stack coherent: documentation precedes code, terminology routes through the selected glossaries, and every artifact links forward (Pact → Rules → Guides → Implementation) so reactive schema work never drifts from architectural intent.

## 2. Principles

- **Continuity:** Stage-gated artifacts persist and evolve; nothing is overwritten without a forward-only rationale.
- **Finesse:** Docs explain why each reactive decision exists (Mutiny session management, module boundaries, etc.).
- **Non-Transactional Flow:** Prompts are “on-call runbooks” for the CRTP stack, not one-off answers.
- **Closing Loops:** Each deliverable links to its upstream rules submodule entry and downstream implementation evidence.

## 3. Structure of Work

| Layer | Description | Artifact |
| --- | --- | --- |
| Pact | Cultural + process alignment for EntityAssistReactive | `PACT.md` |
| Rules | Selected stacks + enforcement details | `RULES.md` (Stage 2) |
| Guides | “How-to” instructions for CRTP builders, DB modules, CI | `GUIDES.md` (Stage 2) |
| Implementation | Evidence that code follows the guides | `IMPLEMENTATION.md` (Stage 3) |

## 4. Behavioral Agreements

1. **Documentation-first:** No source changes until Stage 1 + Stage 2 docs are approved.
2. **Topic-first Glossary:** Host glossary delegates to `rules/generative/backend/guicedee/GLOSSARY.md`, `rules/generative/backend/hibernate/GLOSSARY.md`, `rules/generative/backend/lombok/GLOSSARY.md`, `rules/generative/language/java/GLOSSARY.md`, and `rules/generative/platform/ci-cd/providers/github-actions.md` for canonical phrases.
3. **Traceable diagrams:** Every C4/sequence/ERD diagram lives under `docs/architecture/` with Mermaid sources, linked from docs/PROMPT_REFERENCE.md.
4. **Evidence capture:** Each stage notes unknowns (e.g., actual domain tables) instead of inventing artifacts.

## 5. Technical Commitments

- **Language:** Java 25 (JPMS) with Maven; reactive stack built on Mutiny and Hibernate Reactive.
- **Integrations:** GuicedEE Core/Persistence modules, Vert.x 5 SQL client, Vert.x reactive database drivers (e.g., PostgreSQL), GitHub Actions workflow.
- **Security:** Document trust boundaries between developer workstations, CI secrets, and database endpoints.
- **Testing:** JUnit + GuicedEE testcontainers; coverage tooling deferred until Stage 2 rules specify Jacoco/Sonar.

## 6. Collaboration Flow

1. **Stage 1 (Architecture & Foundations):** Deliver PACT, docs/architecture diagrams, glossary, PROMPT_REFERENCE.
2. **STOP for review.**
3. **Stage 2 (Guides & Design Validation):** RULES + GUIDES referencing selected topics; update README and environment specs.
4. **STOP for review.**
5. **Stage 3 (Implementation Readiness):** IMPLEMENTATION.md, CI/env wiring confirmations, backlog of pending code changes.

## 7. Closing Note

This pact is referenced by all follow-up prompts. Any AI assistant working on EntityAssistReactive must load `PACT.md`, `docs/PROMPT_REFERENCE.md`, and the selected topic rules before proposing code or documentation updates.
