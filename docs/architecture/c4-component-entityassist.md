# C4 Component — EntityAssistReactive Module

```mermaid
C4Component
    title EntityAssistReactive Components
    Container_Boundary(module, "com.entityassist") {
        Component(rootEntity, "RootEntity/BaseEntity", "CRTP Abstract Classes", "Expose fluent persistence API + validation")
        Component(queryBuilder, "QueryBuilder/QueryBuilderRoot", "Hibernate Criteria Composer", "Builds predicates, joins, cache hints")
        Component(converters, "Converters & Enums", "JPA AttributeConverters", "Translate temporal + enum values")
        Component(services, "Service Interfaces", "Interfaces (IRootEntity/IQueryBuilder)", "Define contracts for CRTP implementations")
        Component(guice, "IGuiceContext Integration", "GuicedEE", "Provides builders + Mutiny sessions")
    }
    Container(mut, "Mutiny Session Factory", "Hibernate Reactive", "Produces Session/StatelessSession")
    Container(vertx, "Vert.x Reactive DB Client", "Vert.x 5", "Executes SQL against driver-selected database")
    ContainerDb(pg, "Reactive Database", "Driver-defined", "Tenant schemas")

    Rel(rootEntity, queryBuilder, "instantiates via builder(session)")
    Rel(queryBuilder, converters, "uses for Criteria values")
    Rel(queryBuilder, services, "implements contracts defined in services")
    Rel(guice, queryBuilder, "injects instances + sessions")
    Rel(queryBuilder, mut, "requests Session/StatelessSession")
    Rel(mut, vertx, "delegates reactive IO")
    Rel(vertx, pg, "executes SQL/DDL")
```

## Implementation Notes
- `RootEntity` exposes helper methods (`persist`, `update`) that delegate to the matching query builder and operate on `Uni` of the entity type.
- Query builders rely on `IGuiceContext` to resolve both builders and the shared `Mutiny.SessionFactory`; this necessitates Guice bindings in consuming apps.
- Attribute converters (LocalDate/LocalDateTime) ensure compatibility with JDBC/Vert.x mappings regardless of whether the host uses PostgreSQL, MySQL, or another supported driver.
