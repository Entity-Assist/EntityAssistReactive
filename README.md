# Entity Assist Reactive

CRTP-first reactive persistence module for GuicedEE services. It ships fluent entity/query-builder scaffolding on Vert.x 5, Hibernate Reactive 7, and Vert.x reactive SQL drivers (PostgreSQL by default).

## Documentation-First Workflow
1. Read `PACT.md`, `GLOSSARY.md`, `RULES.md`, `GUIDES.md`, and `docs/PROMPT_REFERENCE.md` before proposing changes.
2. Load the diagrams under `docs/architecture/` (context, container, component, sequences, ERD) to understand trust boundaries.
3. Follow the Pact → Rules → Guides → Implementation chain; record evidence in `IMPLEMENTATION.md` after each change.

## Stack Snapshot
| Layer | Selection |
| --- | --- |
| Language | Java 25 LTS + JPMS |
| Build | Maven |
| Framework | GuicedEE Core + Persistence |
| Reactive Runtime | Vert.x 5 + Hibernate Reactive 7 |
| Drivers | Vert.x reactive SQL drivers (Pg default, swapable) |
| CI/CD | GitHub Actions (`.github/workflows/maven-publish.yml`) |

Topic references live inside `RULES.md` (GuicedEE, Vert.x, Hibernate, Lombok, CI/CD, secrets, observability).

## Quick Start
```bash
cp .env.example .env   # update secrets + DB connection
mvn -B verify          # runs compilation + tests
```

- Provide driver credentials via `.env`/environment variables (`DB_URL`, `DB_USER`, `DB_PASS`, etc.).
- Use Testcontainers with `TEST_DB_CONTAINER_IMAGE` when running integration tests locally.

## Environment & Secrets
- Database connections are supplied via GuicedEE `DatabaseModule` overrides (see `src/test/java/com/test/EntityAssistReactiveDBModule.java` for a template that sets host/user/password inside `getConnectionBaseInfo`). Override `@EntityManager` modules in your service to point to the correct database.
- `.env.example` only captures optional toggles (logging, tests) and CI secrets (GPG/Sonatype credentials). Copy it to `.env` if you need those helpers locally.
- Follow `rules/generative/platform/secrets-config/env-variables.md` for naming conventions and keep GitHub Actions secrets in repository/environment scope.
- Never commit `.env`; rely on secret managers or CI environment secrets instead.

## Architecture & Guides
- Architecture index: `docs/architecture/README.md`
- Guides for CRTP entities, driver selection, Mutiny sessions, and CI usage: `GUIDES.md`
- Implementation evidence and module inventory: `IMPLEMENTATION.md`

## Contributing
- Respect the forward-only policy; supersede docs rather than deleting them.
- Keep the rules submodule (`rules/`) synced before editing prompts.
- All pull requests should include documentation updates when behavior changes.
