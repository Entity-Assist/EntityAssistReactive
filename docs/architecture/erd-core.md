# ERD — EntityAssistReactive Core

```mermaid
erDiagram
    ROOT_ENTITY ||--|| BASE_ENTITY : extends
    BASE_ENTITY ||--|| DEFAULT_ENTITY : extends
    ROOT_ENTITY ||--o{ QUERY_BUILDER : "builder(session)"
    QUERY_BUILDER ||--|| QUERY_BUILDER_ROOT : extends
    QUERY_BUILDER_ROOT ||--o{ JOIN_EXPRESSION : composes
    QUERY_BUILDER_ROOT ||--o{ FILTER_PREDICATE : composes
    QUERY_BUILDER ||--o{ ATTRIBUTE_CONVERTER : "uses"
    ATTRIBUTE_CONVERTER }|..|| POSTGRES_TABLE : "serializes to"
    ROOT_ENTITY ||..|| POSTGRES_TABLE : "maps via @Entity/@Table"
```

## Interpretation
- The ERD represents abstract relationships because concrete entity tables are not included in the repository yet.
- `FILTER_PREDICATE` aggregates Hibernate Criteria predicates derived from enumerations such as `OrderByType`, `Operand`, and `SelectAggregate`.
- Attribute converters wrap date/time conversions that Vert.x reactive drivers (PostgreSQL, MySQL, MSSQL, etc.) expect.

## Next Steps
- Once domain modules land, extend this ERD with actual entity names and relationships; keep the abstract portion for library-level behavior.
