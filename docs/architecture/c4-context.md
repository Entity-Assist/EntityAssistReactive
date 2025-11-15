# C4 Context — EntityAssistReactive

```mermaid
C4Context
    title EntityAssistReactive — Context
    Person(dev, "GuicedEE Developer", "Implements reactive services and domain entities")
    System(c_app, "Consuming GuicedEE Service", "Micro/monolith service that embeds the EntityAssistReactive module")
    System_Boundary(ea, "EntityAssistReactive Library") {
        System(ealib, "EntityAssistReactive", "CRTP query builders + base entities")
    }
    System_Ext(db, "PostgreSQL Cluster", "Holds tenant data via Vert.x Pg Client")
    System_Ext(ci, "GitHub Actions", "Build + release workflow reusing GuicedEE templates")

    Rel(dev, c_app, "Implements features using")
    Rel(c_app, ealib, "Depends on", "Maven + JPMS")
    Rel(ealib, db, "Executes reactive SQL via Mutiny")
    Rel(ci, ealib, "Publishes artifacts / validates docs")
    Rel(dev, ci, "Reviews build results")
```

## Notes
- EntityAssistReactive is consumed as a module rather than a standalone system; the boundary ensures we track where the rules repository applies.
- A Vert.x reactive SQL driver (PostgreSQL via vertx-pg-client today) is the authoritative data source; hosts may swap drivers as required.
- GitHub Actions is both CI/CD and a compliance boundary because GPG/Sonatype secrets transit there.
