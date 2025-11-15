# EntityAssistReactive Glossary (Topic-First)

## Precedence Policy
1. Topic glossaries define canonical language. When a term exists in one of the selected glossaries, reference that entry instead of redefining it.
2. This host glossary acts as an index + alignment guide for CRTP-based persistence discussions. Only EntityAssist-specific terms are defined locally.
3. If a future topic is added (e.g., Angular or WebAwesome), link its glossary here and update RULES.md accordingly.

## Selected Topic Glossaries
- [GuicedEE Stack](rules/generative/backend/guicedee/GLOSSARY.md)
- [Hibernate Reactive 7](rules/generative/backend/hibernate/GLOSSARY.md)
- [Lombok](rules/generative/backend/lombok/GLOSSARY.md)
- [Java LTS](rules/generative/language/java/GLOSSARY.md)
- [GitHub Actions](rules/generative/platform/ci-cd/providers/github-actions.md)

## EntityAssistReactive Terms

| Term | Definition |
| --- | --- |
| **CRTP Entity** | A domain type that extends `RootEntity`/`BaseEntity` and feeds its own type arguments to retain fluent, type-safe methods. The CRTP strategy is mandatory for this repository; @Builder variants are disallowed. |
| **Query Builder Root** | The class hierarchy under `com.entityassist.querybuilder.builders.*` that composes Hibernate Criteria queries with Mutiny sessions. Builders may run in stateful or stateless modes and must be provisioned through `IGuiceContext`. |
| **Mutiny Session Boundary** | The trust boundary between application code and Hibernate Reactive sessions (`Mutiny.Session` or `Mutiny.StatelessSession`). All persistence operations cross this boundary; instrumentation and tracing must hook here. |
| **EntityAssist Module** | The Java module `com.entityassist` exported by `module-info.java`. Hosts must declare `requires transitive com.entityassist` and follow the `opens` guidance documented in RULES.md. |
| **Glossary Alignment Loop** | The requirement for every doc (PACT, RULES, GUIDES, IMPLEMENTATION) to link back to this glossary so terminology remains synchronized with topic glossaries. |

## Unknowns to Clarify
- Domain-specific entities (tables, schemas, column names) are not yet published in this repository; ERD diagrams document the abstract entity/query-builder relationship until modules ship concrete models.
