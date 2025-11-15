# EntityAssistReactive Architecture Index

This directory contains the Stage 1 diagrams and narratives required by the documentation-first workflow. Every artifact is source-controlled (Mermaid/Markdown) and linked from `docs/PROMPT_REFERENCE.md`.

## Diagram Catalog

| Artifact | Purpose |
| --- | --- |
| [c4-context.md](c4-context.md) | Shows how EntityAssistReactive fits between GuicedEE services, developers, GitHub Actions, and Vert.x reactive database drivers (PostgreSQL by default). |
| [c4-container.md](c4-container.md) | Details the major runtime containers: EntityAssist module, consuming GuicedEE service, CI/CD, and the database boundary. |
| [c4-component-entityassist.md](c4-component-entityassist.md) | Breaks the module into CRTP entities, query builders, Mutiny sessions, converters, and Guice wiring. |
| [sequence-persist-flow.md](sequence-persist-flow.md) | Documents the “entity persist” flow from developer call → builder → Mutiny session → Vert.x reactive database driver (e.g., Postgres). |
| [sequence-query-flow.md](sequence-query-flow.md) | Shows a read/query path with filter composition and caching hooks. |
| [erd-core.md](erd-core.md) | Captures the abstract ERD between root entities, query builders, join expressions, and downstream reactive SQL tables (driver-agnostic). |

## Data Flow Summary

1. **Developer / Service Layer → CRTP Entity:** Application code instantiates a concrete entity extending `RootEntity` and sets fields according to domain rules.
2. **Entity → QueryBuilder:** Calls to `entity.builder(session)` request a type-safe `QueryBuilder` instance from `IGuiceContext`, binding `Mutiny.Session` or `Mutiny.StatelessSession`.
3. **QueryBuilder → Hibernate Criteria:** `QueryBuilder` subclasses compose predicates, joins, and cache hints through `QueryBuilderRoot` utilities before executing via Mutiny.
4. **Hibernate Reactive → Reactive SQL Driver:** A Vert.x reactive database client (Pg, MySQL, MSSQL, etc.) transports statements; results surface as `Uni<T>` to the caller.
5. **CI/CD Feedback:** GitHub Actions triggers Maven builds/tests to ensure docs and code stay aligned before publishing artifacts.

## Threat Model & Trust Boundaries

| Boundary | Notes |
| --- | --- |
| Developer Workstation ↔ Repository | Requires enforcement of documentation-first rules; secrets stay out of local config. |
| Service Runtime ↔ Mutiny Session | Hibernate Reactive sessions enforce schema access; builders must set `opens` clauses for reflection. |
| Mutiny Session ↔ Reactive Database | Authentication credentials live in environment variables (.env / GitHub secrets). TLS enforcement is pending; document in Stage 2 RULES. |
| GitHub Actions ↔ EntityAssist Artifacts | GPG + Sonatype credentials referenced in `.github/workflows/maven-publish.yml`; describe handling in `.env.example`/README updates. |

## Dependency & Integration Map

- **Internal Modules:** `com.entityassist` exports converters, enums, exceptions, query builders, and service interfaces. `module-info.java` declares transitive requirements on GuicedEE Vert.x persistence, Hibernate Reactive, Mutiny, and JDBC modules.
- **External Services:** Any Vert.x-supported reactive database driver may host live data (PostgreSQL is the default today); `db/entityAssistTestDB.mv.db` provides an H2-style artifact for local testing.
- **Tooling:** Maven orchestrates builds; Lombok handles annotations; GitHub Actions reuses `GuicedEE/Workflows`. Future docs must describe how to regenerate dependency-reduced POMs without breaking the forward-only policy.

## Follow-Up Checklist

- Confirm future entity modules document their concrete tables and link back to `erd-core.md`.
- Add tracing/metrics guidance in GUIDES.md once instrumentation strategy is selected.
- Keep diagrams synchronized with any new bounded contexts (e.g., command/query segregation, messaging adapters).
