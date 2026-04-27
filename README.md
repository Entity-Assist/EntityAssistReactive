# Entity Assist Reactive

[![Build](https://github.com/Entity-Assist/EntityAssistReactive/actions/workflows/build.yml/badge.svg)](https://github.com/Entity-Assist/EntityAssist/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.entityassist/entity-assist-reactive)](https://central.sonatype.com/artifact/com.entityassist/entity-assist-reactive)
[![Maven Snapshot](https://img.shields.io/nexus/s/com.entityassist/entity-assist-reactive?server=https%3A%2F%2Foss.sonatype.org&label=Maven%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/com/entityassist/entity-assist-reactive/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Modular](https://img.shields.io/badge/Modular-JPMS-green)
![Guice 7](https://img.shields.io/badge/Guice-7-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5-green)
![Maven 4](https://img.shields.io/badge/Maven-4-green)

<!-- Tech icons row -->
![Hibernate Reactive](https://img.shields.io/badge/Hibernate-Reactive_7-59666C?logo=hibernate)
![Mutiny](https://img.shields.io/badge/Mutiny-1.x-0A7)
![GuicedEE](https://img.shields.io/badge/GuicedEE-Persistence-0A7)

CRTP-first, reactive persistence toolkit for [GuicedEE](https://github.com/GuicedEE) services.
Provides a fluent entity and query-builder DSL on top of **Vert.x 5**, **Hibernate Reactive 7**, and **Mutiny**, with PostgreSQL as the default driver via Vert.x reactive SQL clients.
Domain entities and repositories become expressive, type-safe, and truly non-blocking.

Built on [Hibernate Reactive](https://hibernate.org/reactive/) · [Vert.x SQL Client](https://vertx.io/docs/vertx-sql-client/java/) · [Google Guice](https://github.com/google/guice) · [Mutiny](https://smallrye.io/smallrye-mutiny/) · JPMS module `com.entityassist` · Java 25+

## 📦 Installation

```xml
<dependency>
  <groupId>com.entityassist</groupId>
  <artifactId>entity-assist-reactive</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.entityassist:entity-assist-reactive:2.0.0")
```
</details>

## ✨ Features

- **CRTP-shaped entities** — extend `BaseEntity<J, Q, I>` for self-referencing fluent setters and automatic query builder linkage
- **Fluent query builder DSL** — composable `where()`, `or()`, `orderBy()`, `groupBy()`, `join()`, and aggregate projections with full static typing
- **Reactive CRUD with Mutiny** — `persist()`, `update()`, `delete()`, `get()`, `getAll()`, `getCount()` all return `Uni<T>`
- **Dot-notation path filters** — `where("roles.name", Equals, "ADMIN")` resolves relationship paths without explicit `JoinExpression`
- **Pagination and result limiting** — `setFirstResults()` / `setMaxResults()` for offset-based pagination
- **Aggregate projections** — `selectMin()`, `selectMax()`, `selectSum()`, `selectAverage()`, `selectCount()`, `selectCountDistinct()` with optional aliases
- **Join support** — `INNER`, `LEFT`, `RIGHT` joins with on-clause builders and nested join expressions
- **Criteria delete and update** — bulk `delete()` and `truncate()` via JPA Criteria API, with safety guards against unfiltered deletes
- **Stateless session support** — `builder(StatelessSession)` for high-throughput bulk operations
- **Jakarta Bean Validation** — `validateEntity()` returns constraint violations before persistence
- **JPA Attribute Converters** — built-in `LocalDate`, `LocalDateTime`, and `LocalDate↔Timestamp` converters
- **`ActiveFlag` lifecycle enum** — rich status model with ranged query helpers (`getActiveRange()`, `getVisibleRangeAndUp()`, etc.)
- **Cache integration** — `setCacheRegion()` / `setCacheName()` for second-level cache hints on queries
- **JPMS / SPI ready** — fits GuicedEE bootstrap and lifecycle; ServiceLoader-driven module discovery

## 🚀 Quick Start

```bash
cp .env.example .env   # update DB credentials + toggles
mvn -B clean verify    # compilation + tests (uses Testcontainers)
```

## 📐 Architecture

### Type Hierarchy

```
IRootEntity                      IQueryBuilderRoot
  └─ IDefaultEntity                └─ IDefaultQueryBuilder
      └─ IBaseEntity                   └─ IQueryBuilder
          ↑                                ↑
 RootEntity<J,Q,I>              QueryBuilderRoot<J,E,I>
   └─ DefaultEntity<J,Q,I>       └─ DefaultQueryBuilder<J,E,I>
       └─ BaseEntity<J,Q,I>          └─ QueryBuilder<J,E,I>
           ↑                              ↑
    Your Entity                    Your QueryBuilder
```

Every entity class binds to a matching query builder via CRTP generics — the entity knows its builder type and vice-versa.

### Entity ↔ Builder Flow

```
Entity.builder(session)
  └─ Guice.get(QueryBuilderClass)
      ├─ setSession(session)
      ├─ setEntity(this)
      └─ return builder
          ├─ where / or / join / orderBy / groupBy …
          ├─ select / selectMin / selectMax …
          └─ get() / getAll() / getCount() / delete() / persist() / update()
              └─ Returns Uni<T>
```

## 🗺️ Module Graph

```
com.entityassist
 ├── com.guicedee.persistence        (DatabaseModule, SessionFactory wiring)
 ├── com.guicedee.client             (IGuiceContext, Guice injection)
 ├── jakarta.persistence             (JPA Criteria API, @Entity, @Table)
 ├── org.hibernate.reactive          (Mutiny.Session / SessionFactory)
 ├── org.hibernate.orm.core          (Hibernate metamodel, CriteriaBuilder)
 ├── io.smallrye.mutiny              (Uni / Multi reactive types)
 ├── io.vertx.sql.client.pg          (Vert.x PostgreSQL reactive driver)
 └── jakarta.xml.bind                (JAXB for XML binding)
```

## 🧱 Defining a CRTP Entity

```java
@Entity
@Accessors(chain = true)
@Table(name = "entity_class")
public class EntityClass
        extends BaseEntity<EntityClass, EntityClass.EntityClassQueryBuilder, String> {

    @Id
    @Column(name = "id", nullable = false)
    @Getter @Setter
    private String id;

    @Column(name = "name")
    @Getter @Setter
    private String name;

    @Column(name = "description")
    @Getter @Setter
    private String description;

    @Override
    public String getId() { return id; }

    @Override
    public EntityClass setId(String id) {
        this.id = id;
        return this;
    }

    public static class EntityClassQueryBuilder
            extends QueryBuilder<EntityClassQueryBuilder, EntityClass, String> {

        @Override
        public boolean isIdGenerated() {
            return false;
        }
    }
}
```

Entities with relationships work the same way:

```java
@Entity
@Accessors(chain = true)
@Table(name = "entity_class_two")
public class EntityClassTwo
        extends BaseEntity<EntityClassTwo, EntityClassTwo.EntityClassTwoQueryBuilder, String> {

    @Id
    @Column(name = "id", nullable = false)
    @Getter @Setter
    private String id;

    @Column(name = "name")
    @Getter @Setter
    private String name;

    @Column(name = "value")
    @Getter @Setter
    private Integer value;

    @ManyToOne
    @JoinColumn(name = "entity_class_id")
    @Getter @Setter
    private EntityClass entityClass;

    @Override
    public String getId() { return id; }

    @Override
    public EntityClassTwo setId(String id) {
        this.id = id;
        return this;
    }

    public static class EntityClassTwoQueryBuilder
            extends QueryBuilder<EntityClassTwoQueryBuilder, EntityClassTwo, String> {

        @Override
        public boolean isIdGenerated() {
            return false;
        }
    }
}
```

## 🔧 Query Builder DSL

### Persist (Create)

```java
sessionFactory.withSession(session ->
    session.withTransaction(tx ->
        entity.builder(session)
              .persist(entity)
    )
).await().indefinitely();
```

### Find by ID

```java
sessionFactory.withSession(session ->
    new EntityClass()
        .builder(session)
        .find("test1")
        .get()                       // Uni<EntityClass>
).await().indefinitely();
```

### Where / Or / OrderBy

```java
sessionFactory.withSession(session -> {
    var qb = new EntityClass().builder(session);
    return qb
        .where(qb.getAttribute("name"), Operand.Like, "A%")
        .or(qb.getAttribute("name"), Operand.Equals, "Bob")
        .orderBy(qb.getAttribute("name"), OrderByType.ASC)
        .setMaxResults(50)
        .getAll();                   // Uni<List<EntityClass>>
});
```

### Dot-Notation Path Filters (Relationship Traversal)

```java
sessionFactory.withSession(session -> {
    var qb = new EntityClassTwo().builder(session);
    return qb
        .where("entityClass.name", Operand.Equals, "Parent Entity")
        .where("value", Operand.GreaterThan, 10)
        .getAll();
});
```

### Pagination

```java
sessionFactory.withSession(session -> {
    var qb = new EntityClass().builder(session);
    return qb
        .where(qb.getAttribute("name"), Operand.Like, "A%")
        .orderBy(qb.getAttribute("name"), OrderByType.ASC)
        .setFirstResults(0)
        .setMaxResults(20)
        .getAll();
});
```

### Count

```java
sessionFactory.withSession(session -> {
    var qb = new EntityClass().builder(session);
    return qb
        .where(qb.getAttribute("name"), Operand.Like, "A%")
        .getCount();                 // Uni<Long>
});
```

### Aggregate Projections

```java
sessionFactory.withSession(session -> {
    var qb = new EntityClassTwo().builder(session);
    return qb
        .selectMax(qb.getAttribute("value"))
        .get(Integer.class);         // Uni<Integer>
});
```

Available aggregates: `selectMin()`, `selectMax()`, `selectSum()`, `selectSumAsDouble()`, `selectSumAsLong()`, `selectAverage()`, `selectCount()`, `selectCountDistinct()`, `selectColumn()`.

### Joins

```java
sessionFactory.withSession(session -> {
    var parent = new EntityClass().builder(session);
    var child = new EntityClassTwo().builder(session);
    return child
        .join(child.getAttribute("entityClass"), parent, JoinType.INNER)
        .where(parent.getAttribute("name"), Operand.Equals, "Parent Entity")
        .getAll();
});
```

### Bulk Delete

```java
sessionFactory.withSession(session ->
    session.withTransaction(tx -> {
        var qb = new EntityClass().builder(session);
        return qb
            .where(qb.getAttribute("name"), Operand.Equals, "obsolete")
            .delete();               // Uni<Integer> — rows affected
    })
);
```

### Entity Delete

```java
sessionFactory.withSession(session ->
    session.withTransaction(tx ->
        entity.builder(session)
              .delete(entity)        // Uni<EntityClass>
    )
);
```

### Update (Merge)

```java
entity.setName("Updated Name");
sessionFactory.withSession(session ->
    session.withTransaction(tx ->
        entity.builder(session)
              .update()              // Uni<EntityClass>
    )
);
```

### Stateless Sessions

For high-throughput bulk operations where managed state tracking is unnecessary:

```java
sessionFactory.withStatelessSession(session ->
    entity.builder(session)           // uses Mutiny.StatelessSession
          .persist(entity)
);
```

## 🔒 Transactions with Mutiny

```java
sessionFactory.withSession(session ->
    session.withTransaction(tx ->
        new EntityClass().builder(session)
            .persist(new EntityClass().setId("b1").setName("Bob"))
            .chain(() ->
                new EntityClass().builder(session)
                    .find("b1")
                    .get()
            )
            .invoke(found -> log.info("Created and retrieved: {}", found.getName()))
    )
);
```

## ⚙️ Configuration

### Database Module

Database connections are configured via GuicedEE `DatabaseModule` subclasses annotated with `@EntityManager`.
See `src/test/java/com/test/EntityAssistReactiveDBModule.java` for a complete template:

```java
@EntityManager(value = "entityAssistReactive", defaultEm = true)
public class EntityAssistReactiveDBModule
        extends DatabaseModule<EntityAssistReactiveDBModule>
        implements IGuiceModule<EntityAssistReactiveDBModule> {

    @Override
    protected String getPersistenceUnitName() {
        return "entityAssistReactive";
    }

    @Override
    protected ConnectionBaseInfo getConnectionBaseInfo(
            PersistenceUnitDescriptor unit, Properties filteredProperties) {
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        connectionInfo.setServerName("localhost");
        connectionInfo.setPort("5432");
        connectionInfo.setDatabaseName("mydb");
        connectionInfo.setUsername(System.getenv("DB_USER"));
        connectionInfo.setPassword(System.getenv("DB_PASSWORD"));
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);
        return connectionInfo;
    }

    @Override
    protected String getJndiMapping() {
        return "jdbc:entityAssistReactive";
    }
}
```

### JPMS Registration

```java
module my.app {
    requires com.entityassist;
    requires com.guicedee.persistence;

    opens my.app.entities to org.hibernate.orm.core, com.google.guice, com.entityassist;

    provides com.guicedee.client.services.lifecycle.IGuiceModule
        with my.app.MyDatabaseModule;
}
```

### Environment Variables

Copy `.env.example` to `.env` for local development. Keep secrets out of version control.

| Variable | Purpose | Default |
|---|---|---|
| `DB_HOST` | Database hostname | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | — |
| `DB_USER` | Database username | — |
| `DB_PASSWORD` | Database password | — |
| `ENVIRONMENT` | Runtime environment | `dev` |
| `PORT` | Application port | `8080` |
| `TRACING_ENABLED` | Enable distributed tracing | `false` |
| `ENABLE_DEBUG_LOGS` | Enable debug logging | `false` |
| `TEST_DB_CONTAINER_IMAGE` | Testcontainers Postgres image | `postgres:latest` |
| `SKIP_INTEGRATION_TESTS` | Skip integration tests | `true` |

CI secrets (`SONA_USERNAME`, `SONA_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `GITHUB_ACTOR`, `GITHUB_TOKEN`) are managed via GitHub Actions repository/environment secrets.

## 🧪 Testing (Testcontainers)

The test module uses Testcontainers to spin up a PostgreSQL instance automatically:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityAssistReactiveTest {

    private Mutiny.SessionFactory sessionFactory;

    @BeforeAll
    public void setup() {
        IGuiceContext.instance();
        JtaPersistService ps = (JtaPersistService) IGuiceContext.get(
            Key.get(PersistService.class, Names.named("entityAssistReactive")));
        ps.start();

        sessionFactory = IGuiceContext.get(
            Key.get(Mutiny.SessionFactory.class, Names.named("entityAssistReactive")));
    }

    @AfterAll
    public void teardown() {
        JtaPersistService ps = (JtaPersistService) IGuiceContext.get(
            Key.get(PersistService.class, Names.named("entityAssistReactive")));
        ps.stop();
    }

    @Test
    void roundTrip() {
        EntityClass entity = new EntityClass()
            .setId("test1")
            .setName("Test Entity")
            .setDescription("Round-trip test");

        sessionFactory.withSession(session ->
            session.withTransaction(tx ->
                entity.builder(session).persist(entity)
            ).chain(() ->
                new EntityClass().builder(session)
                    .find("test1").get()
            ).invoke(found -> {
                assertNotNull(found);
                assertEquals("test1", found.getId());
            })
        ).await().indefinitely();
    }
}
```

The `EntityAssistReactiveDBModule` test module starts the container and wires connection info via `PostgresConnectionBaseInfo`:

```java
@EntityManager(value = "entityAssistReactive", defaultEm = true)
public class EntityAssistReactiveDBModule extends DatabaseModule<EntityAssistReactiveDBModule>
        implements IGuiceModule<EntityAssistReactiveDBModule> {

    private static final PostgreSQLContainer<?> postgresContainer =
        new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("entityassist_test")
            .withUsername("postgres")
            .withPassword("postgres");

    static { postgresContainer.start(); }

    // ... getConnectionBaseInfo() reads host/port/credentials from the container
}
```

Register the test module via JPMS `provides` in the test `module-info.java`:

```java
module entity.assist.test {
    requires com.entityassist;
    requires com.guicedee.persistence;
    requires org.junit.jupiter.api;
    requires org.testcontainers;

    opens com.test to org.junit.platform.commons, org.hibernate.orm.core,
                       com.google.guice, net.bytebuddy, com.entityassist;

    provides IGuiceModule with com.test.EntityAssistReactiveDBModule;
}
```

## 🔌 SPI Contracts & Key Classes

### Entities

| Class / Interface | Purpose |
|---|---|
| `RootEntity<J,Q,I>` | Root CRTP entity — `builder()`, `persist()`, `update()`, `validateEntity()`, property map |
| `DefaultEntity<J,Q,I>` | Intermediate layer between Root and Base (extension point) |
| `BaseEntity<J,Q,I>` | Primary superclass for user entities; wires JSON auto-detect and builder linkage |
| `IRootEntity` / `IDefaultEntity` / `IBaseEntity` | SPI interfaces for the entity hierarchy |

### Query Builders

| Class / Interface | Purpose |
|---|---|
| `QueryBuilderRoot<J,E,I>` | Root builder — CriteriaBuilder, session management, `persist()`, `update()`, `getAttribute()` |
| `DefaultQueryBuilder<J,E,I>` | Fluent DSL — `where()`, `or()`, `join()`, `orderBy()`, `groupBy()`, selects, aggregates, `find()`, `in()`, `reset()` |
| `QueryBuilder<J,E,I>` | Primary superclass for user builders — `get()`, `getAll()`, `getCount()`, `delete()`, `truncate()`, `getResultStream()`, cache support |
| `IQueryBuilderRoot` / `IDefaultQueryBuilder` / `IQueryBuilder` | SPI interfaces for the builder hierarchy |

### Enumerations

| Enum | Values |
|---|---|
| `Operand` | `Like`, `NotLike`, `Equals`, `NotEquals`, `Null`, `NotNull`, `LessThan`, `LessThanEqualTo`, `GreaterThan`, `GreaterThanEqualTo`, `InList`, `NotInList` |
| `OrderByType` | `ASC`, `DESC` |
| `SelectAggregrate` | `None`, `Min`, `Max`, `Count`, `CountDistinct`, `Sum`, `SumLong`, `SumDouble`, `Avg` |
| `GroupedFilterType` | `And`, `Or` |
| `ActiveFlag` | `Unknown` → `Deleted` → `Active` → `Permanent` (with ranged helpers like `getActiveRange()`, `getVisibleRangeAndUp()`) |

### Expression Builders

| Class | Purpose |
|---|---|
| `WhereExpression` | Resolves a single `where` predicate from attribute + operand + value |
| `GroupedExpression` | Groups `where` / `or` predicates with AND / OR logic |
| `JoinExpression` | Defines a join: attribute, type, optional on-clause builder, optional executor builder |
| `SelectExpression` | Column selection with optional aggregate function and alias |
| `OrderByExpression` | Column + direction for ORDER BY |
| `GroupByExpression` | Column for GROUP BY |

### Converters

| Converter | Mapping |
|---|---|
| `LocalDateAttributeConverter` | `LocalDate` ↔ `java.sql.Date` |
| `LocalDateTimeAttributeConverter` | `LocalDateTime` ↔ `java.sql.Timestamp` |
| `LocalDateTimestampAttributeConverter` | `LocalDate` ↔ `java.sql.Timestamp` |

### Exceptions

| Exception | When |
|---|---|
| `EntityAssistException` | Builder instantiation failure, general entity errors |
| `QueryBuilderException` | Query construction errors |

## 🧰 Troubleshooting & Best Practices

- Always run in a Vert.x context (event loop or worker) when interacting with reactive drivers
- Prefer projections (`selectColumn`, `selectMax`, etc.) for read-heavy paths to reduce entity materialization costs
- Use `setFirstResults()` / `setMaxResults()` for pagination; avoid unbounded loads
- Keep transactions short; chain `Uni` calls and reuse a single session within `withTransaction`
- Bulk `delete()` requires at least one filter — call `truncate()` explicitly if you intend to remove all rows
- Use `Mutiny.StatelessSession` via `builder(statelessSession)` for bulk inserts where change tracking is unnecessary
- Revisit diagrams in `docs/architecture/` when changing relationships or loading strategies

## 🧭 Documentation Home

- Architecture — [`docs/architecture/README.md`](docs/architecture/README.md) indexes the C4/sequence/ERD diagrams
- Prompt Reference — [`docs/PROMPT_REFERENCE.md`](docs/PROMPT_REFERENCE.md)

## 🤝 Contributing

Issues and pull requests are welcome.

- All pull requests should include documentation updates when behavior changes

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
