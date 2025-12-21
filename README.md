# 🧠 Entity Assist Reactive

[![JDK](https://img.shields.io/badge/JDK-25%2B-0A7?logo=java)](https://openjdk.org/projects/jdk/25/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

<!-- Tech icons row -->
![Vert.x](https://img.shields.io/badge/Vert.x-5-4B9?logo=eclipsevertdotx&logoColor=white)
![Hibernate Reactive](https://img.shields.io/badge/Hibernate-Reactive-59666C?logo=hibernate)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?logo=postgresql&logoColor=white)
![Mutiny](https://img.shields.io/badge/Mutiny-1.x-0A7)
![Guice](https://img.shields.io/badge/Guice-Enabled-2F4F4F)
![GuicedEE](https://img.shields.io/badge/GuicedEE-Persistence-0A7)

Entity Assist Reactive is a CRTP-first, reactive persistence toolkit for GuicedEE services. It provides a fluent entity and query DSL on top of Vert.x 5, Hibernate Reactive 7, and Mutiny, with PostgreSQL as the default driver via Vert.x reactive SQL clients. The goal is to make your domain entities and repositories expressive, type-safe, and truly non-blocking.

## 📚 Documentation-First Workflow
1. Read `PACT.md`, `GLOSSARY.md`, `RULES.md`, `GUIDES.md`, and `docs/PROMPT_REFERENCE.md` before proposing changes.
2. Load the diagrams under `docs/architecture/` (context, container, component, sequences, ERD) to understand trust boundaries.
3. Follow the Pact → Rules → Guides → Implementation chain; record evidence in `IMPLEMENTATION.md` after each change.

## 🧩 Stack Snapshot
| Layer | Selection |
| --- | --- |
| Language | Java 25 LTS + JPMS |
| Build | Maven |
| Framework | GuicedEE Core + Persistence |
| Reactive Runtime | Vert.x 5 + Hibernate Reactive 7 + Mutiny |
| Drivers | Vert.x reactive SQL drivers (PostgreSQL default; swappable) |
| CI/CD | GitHub Actions (`.github/workflows/maven-publish.yml`) |

Topic references live inside `RULES.md` (GuicedEE, Vert.x, Hibernate, Lombok, CI/CD, secrets, observability).

## ✨ Features
- CRTP-shaped Entities and fluent Repositories with static typing
- Reactive CRUD with Mutiny Uni/Multi over Hibernate Reactive
- Composable predicates: where/and/or, orderBy, select/projections
- Pagination, streaming, and backpressure-friendly fetch
- Transaction helpers with session propagation
- Relationship helpers (1:N / N:M) and batch operations
- JPMS/SPI ready; fits GuicedEE bootstrap and lifecycle

## 📦 Install (Maven)
Add the dependency to your Maven project:

```xml
<dependency>
  <groupId>com.entityassist</groupId>
  <artifactId>entity-assist-reactive</artifactId>
</dependency>
```

## 🚀 Quick Start
```bash
cp .env.example .env   # update secrets + DB connection
mvn -B verify          # runs compilation + tests
```

- Provide driver credentials via `.env`/environment variables (`DB_URL`, `DB_USER`, `DB_PASS`, etc.).
- Use Testcontainers with `TEST_DB_CONTAINER_IMAGE` when running integration tests locally.

## 🧱 Defining a CRTP Entity
Below is a small illustrative example. Names are representative of the CRTP + fluent approach used by Entity Assist.

```
// Domain entity using CRTP-style base type for fluent setters
@Entity
@Table(name = "users")
public class User extends EAEntity<User> { // EAEntity<T> provided by Entity Assist
  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String displayName;

  // Fluent setters return the concrete type User
  public User email(String v) { this.email = v; return this; }
  public User displayName(String v) { this.displayName = v; return this; }

  // getters omitted for brevity
}
```

## 📦 Query Builder and Fluent DSL
The public API uses builders attached to your entity types. You compose queries with `where(...)`, logical `or(...)`, and `orderBy(...)`. For joins, use dot-notation on attribute paths.

```
import static com.entityassist.enumerations.Operand.*;
import static com.entityassist.enumerations.OrderByType.*;

Mutiny.SessionFactory sessionFactory = ...; // obtain via Guice (see tests)

// Create
sessionFactory.withSession(session ->
  session.withTransaction(tx ->
    new User().builder(session)
              .persist(new User().email("a@ex.com").displayName("Alice"))
  )
).invoke(u -> log.info("Persisted user id={} email={}", u.getId(), u.getEmail()));

// Read by id
sessionFactory.withSession(session ->
  new User().builder(session)
            .find(42L)
            .get() // Uni<User>
).invoke(user -> log.info("Loaded user: {}", user));

// Query with where/or/orderBy/max results
sessionFactory.withSession(session -> {
  var qb = new User().builder(session);
  return qb
    .where("email", Equals, "a@ex.com")
    .or(qb.getAttribute("displayName"), Like, "Ali%")
    .orderBy(qb.getAttribute("displayName"), ASC)
    .setMaxResults(50)
    .getAll(); // Uni<List<User>>
}).invoke(list -> log.info("Query returned {} users", list.size()));

// Pagination via first/max
sessionFactory.withSession(session -> {
  var qb = new User().builder(session);
  return qb
    .where(qb.getAttribute("displayName"), Like, "A%")
    .orderBy(qb.getAttribute("displayName"), ASC)
    .setFirstResults(0)
    .setMaxResults(20)
    .getAll();
}).invoke(page -> log.info("Fetched page of {} users", page.size()));
```

## 🔗 Joins (dot-notation) and Filtering
Entity Assist supports dot-notation for relationship paths in `where(...)`. This removes the need for an explicit `JoinExpression` in many cases.

```
import static com.entityassist.enumerations.Operand.*;

sessionFactory.withSession(session -> {
  var qb = new User().builder(session);
  return qb
    // Suppose User has @OneToMany Set<Role> roles with a "name" column
    .where("roles.name", Equals, "ADMIN")
    .where("email", Like, "%@ex.com")
    .getAll();
}).invoke(list -> log.info("Found {} admin users with company email", list.size()));
```

<!-- Streaming can be achieved by batching and paginating results. For large scans, prefer using setFirstResults/setMaxResults in a loop. -->

## 🔒 Transactions with Mutiny
```
// Transactional unit of work using Mutiny API
sessionFactory.withSession(session ->
  session.withTransaction(tx ->
    new User().builder(session)
              .persist(new User().email("b@ex.com").displayName("Bob"))
              .chain(() -> new User().builder(session)
                                      .where("email", Equals, "b@ex.com")
                                      .setMaxResults(1)
                                      .getAll())
              .invoke(list -> log.info("Transaction fetched {} user(s)", list.size()))
  )
);
```

## 🧪 Testing (Testcontainers)
```
@QuarkusTest // or your preferred runner; example only
public class UserRepositoryTest {
  static PostgreSQLContainer<?> db = new PostgreSQLContainer<>(System.getenv().getOrDefault(
    "TEST_DB_CONTAINER_IMAGE", "postgres:16"));

  @BeforeAll static void start() { db.start(); }
  @AfterAll static void stop() { db.stop(); }

  @Inject UserRepository users;

  @Test void roundTrip() {
    User u = new User().email("t@ex.com").displayName("Testy");
    users.persist(u)
      .chain(v -> users.findById(v.getId()))
      .invoke(opt -> {
        assertTrue(opt.isPresent());
        log.info("Round-trip OK for user id={} ", v.getId());
      });
  }
}
```

## ⚙️ Configuration & Secrets
- Database connections are supplied via GuicedEE `DatabaseModule` overrides (see `src/test/java/.../EntityAssistReactiveDBModule.java` for a template that sets host/user/password inside `getConnectionBaseInfo`). Override `@EntityManager` modules in your service to point to the correct database.
- `.env.example` captures toggles (logging, tests) and CI secrets (GPG/Sonatype credentials). Copy it to `.env` if you need those helpers locally.
- Follow `rules/generative/platform/secrets-config/env-variables.md` for naming conventions and keep GitHub Actions secrets in repository/environment scope.
- Never commit `.env`; rely on secret managers or CI environment secrets instead.

## 🧭 Architecture & Guides
- Architecture index: `docs/architecture/README.md`
- Guides for CRTP entities, driver selection, Mutiny sessions, and CI usage: `GUIDES.md`
- Implementation evidence and module inventory: `IMPLEMENTATION.md`

## 🧰 Troubleshooting & Best Practices
- Always run in a Vert.x context (event loop or worker) when interacting with reactive drivers.
- Prefer projections for read-heavy paths to reduce entity materialization costs.
- For large scans, use pagination with `setFirstResults`/`setMaxResults`; avoid unbounded loads.
- Keep transactions short; chain `Uni`/`Multi` and reuse a single session within `withTransaction`.
- Revisit diagrams in `docs/architecture/*` when changing relationships or loading strategies.

## 🤝 Contributing
- Respect the forward-only policy; supersede docs rather than deleting them.
- Keep the rules submodule (`rules/`) synced before editing prompts.
- All pull requests should include documentation updates when behavior changes.
