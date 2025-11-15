# EntityAssistReactive IMPLEMENTATION

This document records the current state of the repository and links every implemented concern back to the rules, guides, and diagrams.

## 1. Module Inventory
| Path | Purpose | Notes |
| --- | --- | --- |
| `src/main/java/com/entityassist/RootEntity.java` | Base CRTP entity with persistence helpers | Uses Mutiny sessions + validation; referenced in `docs/architecture/c4-component-entityassist.md`. |
| `src/main/java/com/entityassist/BaseEntity.java` | Extends `DefaultEntity` for richer behavior | Maintains CRTP signature and logging helpers. |
| `src/main/java/com/entityassist/querybuilder/QueryBuilder.java` | Fluent criteria builder | Implements caching, joins, select/delete logic. |
| `src/main/java/com/entityassist/querybuilder/builders/QueryBuilderRoot.java` | Lower-level criteria utilities | Manages `CriteriaBuilder`, delete/update statements, stateless sessions. |
| `src/main/java/com/entityassist/converters/*.java` | Attribute converters | Align with ERD serialization to reactive SQL tables. |
| `src/main/java/com/entityassist/services/**` | Service contracts | Define `IRootEntity`, `IQueryBuilder`, etc., ensuring fluent APIs are consistent. |
| `module-info.java` | JPMS declarations | Requires transitive GuicedEE persistence, Hibernate Reactive, Vert.x SQL client. |

## 2. Documentation Links
- Architecture diagrams: `docs/architecture/README.md` (C4, sequences, ERD).
- Rules + Guides: `RULES.md`, `GUIDES.md` (Stage 2), `GLOSSARY.md`.
- Prompt coordination: `docs/PROMPT_REFERENCE.md` defines the stacks that future prompts must load.

## 3. Reactive Flow Summary
1. Service code instantiates or injects a CRTP entity (`RootEntity` derivative).
2. `entity.builder(session)` retrieves a Guice-provisioned `QueryBuilder` bound to the Mutiny session (stateful or stateless).
3. Builders configure joins, filters, cache hints, and then execute via Mutiny, producing `Uni<T>` results. These helpers never return `null`; any Hibernate exceptions bubble through the `Uni` so callers can handle them.
4. Vert.x reactive drivers transport SQL commands; driver selection is tracked in `docs/PROMPT_REFERENCE.md` and `README.md` rather than environment variables.
5. GitHub Actions (`.github/workflows/maven-publish.yml`) runs Maven verify/publish when secrets are present.

## 4. Environment & CI Integration
- `.env.example` only stores optional toggles and CI secrets because database credentials are injected via GuicedEE `DatabaseModule` overrides.
- GitHub Actions inherits secrets defined in repository settings; refer to `rules/generative/platform/ci-cd/providers/github-actions.md`.
- GPG/Sonatype credentials must be present before running the publish workflow; otherwise, builds stay in verify-only mode.

## 5. Pending/Next Steps
- Stage 3 will extend this document with implementation evidence (e.g., concrete bounded contexts, repositories using this library, diagrams per new modules).
- When new features (multi-tenant patterns, custom drivers) are added, append sections referencing updated diagrams and guides.

## 6. Stage 3 Implementation Plan (No Code)

### 6.1 Scaffolding & Module Map
- **Core module (`src/main/java/com/entityassist/**`)** — Already contains CRTP entities, query builders, converters, and service interfaces. Stage 4 tasks should map directly to these packages (e.g., add domain-specific entities under `com.entityassist.services.entities` or new builders under `com.entityassist.querybuilder.builders`).
- **JPMS descriptor (`src/main/java/module-info.java`)** — Any new exports/opens must be recorded here and mirrored in `docs/architecture/c4-component-entityassist.md`.
- **Database module examples (`src/test/java/com/test/EntityAssistReactiveDBModule.java`)** — Use this pattern to scaffold production `@EntityManager` modules in host services; wiring lives outside this repo but follows the same GuicedEE factory contract.
- **Docs tree (`docs/architecture`, `RULES.md`, `GUIDES.md`)** — Stage 4 code updates must reference these artifacts when introducing new flows or terminology to keep the documentation loop closed.
- **Test module (`src/test/java/module-info.java`)** — Shows the required `requires`/`opens` clauses for the JPMS test module (`module entity.assist.test`) plus the `provides` entry for `IGuiceModule`. Mirror this structure (including `opens com.test ... org.hibernate.orm.core`) when creating additional tests to keep reflective access working.

### 6.2 Build & Annotation Processor Wiring
- `pom.xml` already wires `maven-compiler-plugin` with Lombok (1.18.40) and Hibernate processor (`${maven.hibernate.version}`) plus explicit `--add-reads` arguments. Stage 4 changes must reuse this configuration; new processors should be added to the same plugin block and documented in `RULES.md`.
- `maven-surefire-plugin` propagates JPMS flags via `<argLine>--add-reads org.hibernate.orm.core=entity.assist.test --add-reads org.jboss.logging=org.hibernate.reactive</argLine>`. Future test runners or profiles must keep these arguments (or their equivalents) so Hibernate Reactive can access the test module.
- `flatten-maven-plugin` and `copy-rename-maven-plugin` are present; confirm before release whether they require configuration updates for new modules.
- Dependencies rely on the GuicedEE BOM (`dependencyManagement`). Any new dependency must align with the BOM versions to avoid drift; document additions in `GUIDES.md` before editing the POM.

### 6.3 CI Workflow & Validation
- `.github/workflows/maven-publish.yml` delegates to `GuicedEE/Workflows/.github/workflows/projects.yml@master`. Stage 4 tasks should anticipate the existing inputs (e.g., `centralRelease`, `publishToCentral`) and note required secrets (USERNAME, USER_TOKEN, SONA_*, GPG_*).
- Validation path: `mvn -B verify` locally and in CI; for reactive DB flows include Testcontainers-based tests with `TEST_DB_CONTAINER_IMAGE`. Add any new workflow requirements (additional jobs, linting) by extending the README + RULES before altering YAML.

### 6.4 Environment & Config Plan
- Database connections remain in GuicedEE `DatabaseModule` overrides (see Section 6.1). Future implementation work should deliver production-ready modules (per environment) and update GUIDES with credential source (Secret Manager, Vault, etc.).
- `.env` usage is limited to toggles/secrets listed in `.env.example`; Stage 4 tasks must note additional keys there and in `rules/generative/platform/secrets-config/env-variables.md`.
- Document how environment stacks feed into CI (e.g., GitHub Environments for release vs. snapshot) before changing workflows.

### 6.5 Rollout Plan, Risks, and Validation Approach
- **Phase 1 (Foundation)** — Introduce any new CRTP entities/builders plus DatabaseModule overrides in host services; validate with integration tests using Mutiny sessions and Testcontainers. Update diagrams if module boundaries change.
- **Phase 2 (Feature rollout)** — Wire services to the new builders, ensure JPMS requirements are satisfied (add `opens`/`requires`), and document the flows in `GUIDES.md` + `IMPLEMENTATION.md`.
- **Phase 3 (Release readiness)** — Confirm CI secrets, verify `mvn -B verify` + publishing workflow, and capture release notes referencing the documentation chain.
- **Risks:** JPMS `--add-reads` errors if new modules are not declared; mismatched driver configuration if DatabaseModule overrides are inconsistent; CI failures when secrets are missing; documentation drift if diagrams are not updated alongside code.
- **Validation:** For each phase, tie acceptance back to Stage 1/2 artifacts—e.g., confirm new features align with the ERD and sequence diagrams, ensure RULES coverage for additional stacks, and record proof in `IMPLEMENTATION.md`.
