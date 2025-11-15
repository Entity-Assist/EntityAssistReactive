# EntityAssistReactive RULES

These rules extend the organization-wide policies in `rules/RULES.md` and the adopted PACT. They focus on keeping the CRTP-based reactive persistence stack aligned with the selected topics, diagrams, and glossary.

## 1. Scope & Stage Gates
- Follow the documentation-first workflow: Stage 1 (architecture) → Stage 2 (rules & guides) → Stage 3 (implementation evidence). Code changes must not start until the earlier stages are reviewed.
- Reference chain: `PACT.md` → `GLOSSARY.md` → this file → `GUIDES.md` → `IMPLEMENTATION.md`. Every doc must link forward/back to maintain traceability.

## 2. Selected Stacks & Source Docs
| Layer | Decision | Rules Reference |
| --- | --- | --- |
| Language | Java 25 LTS | `rules/generative/language/java/README.md`, `rules/generative/language/java/java-25.rules.md` |
| Build | Maven (JPMS modules) | `rules/generative/language/java/build-tooling.md` |
| Framework | GuicedEE Core + Persistence | `rules/generative/backend/guicedee/README.md` |
| Fluent Strategy | CRTP only (no Lombok builders) | `rules/generative/backend/fluent-api/README.md` |
| Reactive Runtime | Vert.x 5 + Hibernate Reactive 7 | `rules/generative/backend/vertx/README.md`, `rules/generative/backend/hibernate/README.md` |
| Drivers | Vert.x reactive SQL drivers (Pg default, no “SQL Client templates” scaffolding) | `rules/generative/backend/vertx/vertx-5-postgres-client.md`, `rules/generative/data/database/postgres-database.md`, `rules/generative/backend/vertx/vertx-5-transaction-handling.md` |
| Data Guidance | EntityAssist domain guidance | `rules/generative/data/entityassist/README.md` |
| Structural | Lombok + logging guidance | `rules/generative/backend/lombok/README.md`, `rules/generative/backend/logging/README.md` |
| Observability | OpenAPI + health/tracing templates | `rules/generative/platform/observability/README.md`, `rules/generative/platform/observability/health.md`, `rules/generative/platform/observability/openapi.md` |
| CI/CD | GitHub Actions workflow templates | `rules/generative/platform/ci-cd/README.md`, `rules/generative/platform/ci-cd/providers/github-actions.md` |
| Security | Secrets/OIDC guidance | `rules/generative/platform/security-auth/README.md`, `rules/generative/platform/secrets-config/env-variables.md` |
| Architecture Process | Base, TDD, BDD rules | `rules/generative/architecture/README.md`, `rules/generative/architecture/tdd/README.md`, `rules/generative/architecture/bdd/README.md` |

## 3. Topic Links (Must Stay in Sync)
- GuicedEE Core & Services
  - `rules/generative/backend/guicedee/README.md`
  - `rules/generative/backend/guicedee/functions/guiced-injection-rules.md`
  - `rules/generative/backend/guicedee/functions/guiced-vertx-persistence-rules.md`
- Reactive Persistence
  - `rules/generative/backend/hibernate/README.md`
  - `rules/generative/backend/vertx/README.md`
  - `rules/generative/backend/security-reactive/README.md`
- Data + EntityAssist
  - `rules/generative/data/entityassist/README.md`
  - `rules/generative/data/database/postgres-database.md` (extend/override when swapping Vert.x drivers)
- Structural Topics
  - `rules/generative/backend/lombok/README.md`
  - `rules/generative/backend/fluent-api/README.md`
- Platform/CI & Observability
  - `rules/generative/platform/ci-cd/README.md`
  - `rules/generative/platform/ci-cd/providers/github-actions.md`
  - `rules/generative/platform/observability/README.md`
  - `rules/generative/platform/secrets-config/env-variables.md`
  - `rules/generative/platform/security-auth/README.md`

## 4. Architecture Alignment
- Diagrams referenced in `docs/architecture/README.md` are the authoritative context. Any new module or feature must update those diagrams.
- Use the ERD + C4 diagrams to justify module boundaries before modifying `module-info.java` or introducing new packages.

## 5. Glossary Precedence
- Apply the topic-first policy defined in `GLOSSARY.md`. Terms like “CRTP Entity”, “Query Builder Root”, “Mutiny Session Boundary” must match the glossary and linked topic glossaries.
- When a conflicting term appears in an external dependency, add a note to `GLOSSARY.md` rather than redefining it inline.

## 6. Module & Build Rules
1. `com.entityassist` must export only the packages documented in `module-info.java`; new exports require documentation + diagram updates.
2. GuicedEE integrations (`IGuiceContext`, `Mutiny.SessionFactory`) are injected via modules defined in host applications; never instantiate them manually.
3. Provide database credentials through GuicedEE `DatabaseModule` implementations (`@EntityManager` + `getConnectionBaseInfo`); never override `getEntityManager()` in builders—the method simply returns the Mutiny session supplied by Guice.
4. Use CRTP signatures consistently: `<J extends RootEntity<J, Q, I>, Q extends QueryBuilderRoot<Q, J, I>, I extends Serializable>`.
5. Lombok is limited to annotations already in the repo. Do not introduce `@Builder`; prefer explicit fluent APIs per CRTP strategy.
6. All persistence operations must return Mutiny `Uni<?>` to keep APIs reactive, and the builder helpers (`persist`, `update`, `delete`, etc.) must never return `null`. They simply propagate Hibernate Reactive/Mutiny exceptions back to the caller—do not swallow, wrap, or convert them unless explicitly documented.
7. Downstream projects must add the Hibernate annotation processor (`org.hibernate.orm:hibernate-processor`) to their compiler plugin and include the same `--add-reads` arguments plus `opens` clauses (`org.hibernate.orm.core=<your.module.name>`, `org.jboss.logging=org.hibernate.reactive`) shown in this repository’s `pom.xml` and test `module-info.java`.

## 7. Environment & Secrets
- Database connections must be supplied by GuicedEE `DatabaseModule` overrides (see `src/test/java/com/test/EntityAssistReactiveDBModule.java`); `.env.example` only captures optional toggles and release secrets per `rules/generative/platform/secrets-config/env-variables.md`.
- GitHub Actions secrets (`USERNAME`, `USER_TOKEN`, `SONA_*`, `GPG_*`) must be set before running `.github/workflows/maven-publish.yml`. Document usage inside README + `.env.example` comments.

## 8. CI/CD & Release Policy
- Reuse `GuicedEE/Workflows` only; custom workflows must be reviewed to ensure they follow `rules/generative/platform/ci-cd/providers/github-actions.md`.
- Every build step must run `mvn -B verify`. Publishing requires `.env` values for signing/GPG to be present.

## 9. Forward-Only Changes
- Never rewrite history or delete docs. Instead, supersede sections with dated notes while linking to previous versions.
- When replacing diagrams or guides, keep older versions under `docs/architecture/archive/` (if needed) and reference the rationale here.
