# Sequence — Persisting a CRTP Entity

```mermaid
sequenceDiagram
    participant Dev as Service Code
    participant Entity as Concrete RootEntity
    participant Builder as QueryBuilder
    participant Mutiny as Mutiny.Session
    participant Db as Vert.x Reactive DB

    Dev->>Entity: populate fields + validations
    Dev->>Entity: builder(MutinySession)
    Entity-->>Builder: type-safe QueryBuilder instance via IGuiceContext
    Builder->>Builder: select()/processCriteriaQuery()
    Builder->>Mutiny: persist(entity)
    Mutiny->>Db: Parameterized INSERT/UPDATE via Vert.x reactive driver
    Db-->>Mutiny: ACK / generated id
    Mutiny-->>Builder: Uni<J>
    Builder-->>Entity: Uni<J>
    Entity-->>Dev: Uni<J> (reactive completion)
```

## Highlights
- `builder(session)` obtains an injected builder, preventing manual construction mistakes.
- Cache hints and join processing occur before the Mutiny session call (per `QueryBuilder.select()` logic).
- All persistence returns a `Uni<J>`, so services should compose with Mutiny rather than blocking.
