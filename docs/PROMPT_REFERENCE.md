# Prompt Reference — EntityAssistReactive

This file must be loaded before using any AI assistant on the repository. It summarizes selections, diagrams, and rule entry points made during Stage 1.

## Stack + Topic Selections
- **Language:** Java 25 LTS (`rules/generative/language/java/README.md`)
- **Build:** Maven (`rules/generative/language/java/build-tooling.md`)
- **Backend Reactive:** Vert.x 5 (`rules/generative/backend/vertx/README.md`), Hibernate Reactive 7 (`rules/generative/backend/hibernate/README.md`), GuicedEE Core + Persistence (`rules/generative/backend/guicedee/README.md`)
- **Database:** Vert.x reactive database drivers (PostgreSQL via vertx-pg-client today; swapable with other supported drivers). Use the closest matching rules doc such as `rules/generative/data/database/postgres-database.md` or equivalent when another driver is selected.
- **Structural:** Lombok (`rules/generative/backend/lombok/README.md`), logging via Log4j2/JUL bridging.
- **CI/CD:** GitHub Actions (`rules/generative/platform/ci-cd/providers/github-actions.md`)
- **Security/Secrets:** `.env.example` (to be authored in Stage 2) referencing `rules/generative/platform/secrets-config/env-variables.md`.

## Documentation Loop
- `PACT.md` → Defines collaboration + topic-first glossary policy.
- `GLOSSARY.md` → Indexes selected glossaries + EntityAssist-specific terminology.
- `docs/architecture/` → Holds diagrams referenced by RULES/GUIDES/IMPLEMENTATION.
- `RULES.md`, `GUIDES.md`, `IMPLEMENTATION.md` → Stage 2 artifacts that describe enforcement, how-to steps, and current implementation evidence; review them before any code change.

## Environment Snapshot
- Database host/user/password are provided by GuicedEE `DatabaseModule` overrides (see `src/test/java/com/test/EntityAssistReactiveDBModule.java`).
- `.env.example` captures only optional toggles (logging/test flags) and CI/CD secrets; copy it when you need those helpers locally.

## Diagram Index (load before proposing code changes)
1. [C4 Context](architecture/c4-context.md)
2. [C4 Container](architecture/c4-container.md)
3. [C4 Component](architecture/c4-component-entityassist.md)
4. [Sequence — Persist Flow](architecture/sequence-persist-flow.md)
5. [Sequence — Query Flow](architecture/sequence-query-flow.md)
6. [ERD — Core Relationships](architecture/erd-core.md)

## Evidence Requirements
- Capture unknowns (e.g., actual domain schemas) explicitly rather than inferring; update ERD when new modules arrive.
- When editing prompts or docs, mention the rules submodule commit if it changes.
- Respect stage gates: Stage 1 artifacts (this file plus diagrams) must be reviewed before moving to Stage 2 documentation or code edits.
