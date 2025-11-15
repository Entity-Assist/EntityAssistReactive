# Sequence — Query with Filters & Cache

```mermaid
sequenceDiagram
    participant Service as Vert.x Resource Handler
    participant Entity as CRTP Entity
    participant Builder as QueryBuilder
    participant Mutiny as Mutiny.Session
    participant Cache as Hibernate/Vert.x Cache
    participant Db as Vert.x Reactive DB

    Service->>Entity: builder(session)
    Entity-->>Builder: QueryBuilder with entity reference
    Service->>Builder: filterByTenant() / joinRelation()
    Builder->>Builder: selectCount()/select()
    alt Cache configured
        Builder->>Cache: register cache region + hints
    end
    Builder->>Mutiny: getQuery().getResultList()
    Mutiny->>Db: Execute SELECT with Criteria + joins
    Db-->>Mutiny: Row set
    Mutiny-->>Builder: Uni<List<E>>
    Builder-->>Service: Uni<List<E>>
```

## Highlights
- `QueryBuilder` lazily composes the criteria query; `getQuery()` ensures `select()` ran.
- Cache hints only apply when `setCacheName`/`setCacheRegion` are configured.
- Service handlers must subscribe to `Uni` inside Vert.x event-loop friendly contexts.
