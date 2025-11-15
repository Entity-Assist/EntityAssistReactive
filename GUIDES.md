# EntityAssistReactive GUIDES

This file explains how to apply the rules in practical workflows. Each section links to the topic references defined in `RULES.md`, the glossary, and the Stage 1 diagrams.

## 1. Creating CRTP Entities & Builders
1. Review `docs/architecture/sequence-persist-flow.md` to understand the runtime path.
2. Define your entity by extending `RootEntity` or `BaseEntity` with the CRTP signature. Keep fields annotated with JPA + Bean Validation.
3. Implement a matching `QueryBuilder` that extends `com.entityassist.querybuilder.QueryBuilder` (or `QueryBuilderRoot` for advanced cases) and injects via `IGuiceContext`.
4. Add Guice bindings for the builder in the host service module per `rules/generative/backend/guicedee/functions/guiced-injection-rules.md`.
5. Update `module-info.java` exports/opens if new packages are introduced, and document them in `IMPLEMENTATION.md`.
6. Builder helper methods (`persist`, `update`, `delete`) always yield a `Uni<J>` containing the entity (never `null`) and simply propagate Hibernate Reactive exceptions—compose them with Mutiny error handling rather than wrapping inside the builder.

## 2. Configuring Database Modules / Drivers
1. Database connections are wired through GuicedEE `DatabaseModule` implementations annotated with `@EntityManager`. See `src/test/java/com/test/EntityAssistReactiveDBModule.java` for a Postgres Testcontainers example.
2. Override `getConnectionBaseInfo` to supply host, port, credentials, and `setReactive(true)`; `getEntityManager()` in `QueryBuilderRoot` simply returns the Mutiny session injected via this module.
3. To change drivers, follow `rules/generative/backend/vertx/README.md` plus the driver-specific template (e.g., `rules/generative/backend/vertx/vertx-5-postgres-client.md`) and adjust your `ConnectionBaseInfo` implementation accordingly.
4. Record the driver choice in `docs/PROMPT_REFERENCE.md` and update ERD/diagrams if new capabilities (clustered writes, sharding) are introduced.

## 3. Managing Mutiny Sessions
1. Obtain sessions from `IGuiceContext`-managed factories; never instantiate `Mutiny.SessionFactory` manually.
2. For read/write flows, mirror `docs/architecture/sequence-query-flow.md` to ensure `select()` is invoked before query execution.
3. Cache hints (set via `QueryBuilder`) must align with the Hibernate Reactive rules in `rules/generative/backend/hibernate/README.md`.
4. Any time new cache regions or transaction strategies are introduced, create a corresponding subsection in `IMPLEMENTATION.md` and reference the relevant guide.

## 4. Environment Configuration
- Use `.env.example` as the canonical reference for local `.env` creation.
- Database keys: `DB_URL`, `DB_USER`, `DB_PASS`, `DB_POOL_SIZE`, `VERTX_DB_DRIVER`. Override per environment using Terraform/secret managers as described in `rules/generative/platform/secrets-config/env-variables.md`.
- CI/CD secrets: `SONA_USERNAME`, `SONA_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `GITHUB_TOKEN`. Map these to GitHub Actions environment secrets before running the workflow.

## 5. Running + Testing
1. `mvn -B verify` is the baseline command locally and in GitHub Actions (`.github/workflows/maven-publish.yml`).
2. For integration tests, provide `TEST_DB_CONTAINER_IMAGE` and `SKIP_INTEGRATION_TESTS=false` in `.env` so Testcontainers can use the correct driver.
3. The test JPMS module (`src/test/java/module-info.java`) shows the required `requires`/`opens` clauses for `module entity.assist.test`; new tests must extend this pattern so Hibernate Reactive, Guice, and JUnit can reflectively access entities.
4. The Maven Surefire plugin passes `--add-reads org.hibernate.orm.core=entity.assist.test --add-reads org.jboss.logging=org.hibernate.reactive`. Preserve (or expand) those flags whenever you create new test runners/profiles so JPMS constraints are satisfied.

## 6. Documentation Loop
- Before proposing code changes, load `PACT.md`, `GLOSSARY.md`, `docs/PROMPT_REFERENCE.md`, and these guides.
- After implementing a guide, capture the change in `IMPLEMENTATION.md` with links back to diagrams/guides to keep the Pact → Rules → Guides → Implementation chain intact.
- Ensure consuming projects add the Hibernate annotation processor (`org.hibernate.orm:hibernate-processor`) to their `maven-compiler-plugin` (or equivalent) alongside Lombok so CRTP entities generate metadata correctly. This repository already models the configuration in `pom.xml`; mirror it before expecting builders to compile.
- All consuming JPMS modules must include the necessary `--add-reads` arguments (`org.jboss.logging=org.hibernate.reactive`, `org.hibernate.orm.core=<your.module.name>`) and `opens` clauses so Hibernate Reactive can access entity packages. See `module-info.java` in test sources for an example mapping.
