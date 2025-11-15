# C4 Container — Reactive Stack

```mermaid
C4Container
    title Containers using EntityAssistReactive
    Person(dev, "GuicedEE Developer")

    System_Boundary(app, "Consuming Service") {
        Container(vertx, "Vert.x Reactive Service", "Java 25 / GuicedEE", "Hosts business modules and injects EntityAssistReactive")
        ContainerDb(pg, "PostgreSQL", "Managed cluster", "Tenant + metadata tables")
        Container(ea, "EntityAssistReactive Module", "Jar (com.entityassist)", "CRTP entities, query builders, converters")
    }

    System_Boundary(ci, "CI/CD") {
        Container(gha, "GitHub Actions Workflow", "GuicedEE/Workflows", "Runs Maven verify/publish with secrets")
        Container(rules, "Rules Submodule", "git submodule", "Policies + prompts consumed by AI")
    }

    Rel(dev, vertx, "Implements features and runs tests")
    Rel(vertx, ea, "requires transitive")
    Rel(ea, pg, "SQL over Vert.x Pg + Mutiny")
    Rel(gha, vertx, "Builds + signs artifacts")
    Rel(gha, rules, "Syncs documentation policies")
    Rel(dev, rules, "Reads prompts to drive docs")
```

## Observations
- The EntityAssist module sits inside the consuming Vert.x container but maintains its own lifecycle (JPMS exports/opens).
- GitHub Actions consumes both source and rules; documentation updates must keep `.gitmodules` intact.
- Reactive database credentials pass through environment variables; `.env.example` (Stage 2) will outline expected names regardless of the underlying driver.
