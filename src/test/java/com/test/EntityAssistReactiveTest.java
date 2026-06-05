package com.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.persistence.PersistService;
import com.guicedee.persistence.bind.JtaPersistService;
import com.entityassist.enumerations.Operand;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Log4j2
public class EntityAssistReactiveTest
{
    private Mutiny.SessionFactory sessionFactory;

    @BeforeAll
    public void setup()
    {
        // Initialize the Guice context
        IGuiceContext.instance();
        log.info("Attempting to get PersistService from Guice");
        JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("entityAssistReactive")));
        assertNotNull(ps, "PersistService should not be null");
        ps.start();

        // Get the session factory
        sessionFactory = IGuiceContext.get(Key.get(Mutiny.SessionFactory.class, Names.named("entityAssistReactive")));
        assertNotNull(sessionFactory, "SessionFactory should not be null");
    }

    @AfterAll
    public void afterAll()
    {
        JtaPersistService ps = (JtaPersistService) IGuiceContext.get(Key.get(PersistService.class, Names.named("entityAssistReactive")));
        ps.stop();
    }

    @Test
    public void testEntityClassPersistence()
    {
        // Create a new entity
        EntityClass entity = new EntityClass()
                                     .setId("test1")
                                     .setName("Test Entity")
                                     .setDescription("This is a test entity")
                ;

        // Use sessionFactory to start a reactive session and transaction
        sessionFactory.withSession(session -> {
            // Persist the entity
            return session.withTransaction(tx -> {
                return entity.builder(session)
                             .persist(entity)
                             .invoke(persisted -> {
                                 // Verify the entity was persisted
                                 assertNotNull(persisted);
                                 assertEquals("test1", persisted.getId());
                                 assertEquals("Test Entity", persisted.getName());
                                 assertEquals("This is a test entity", persisted.getDescription());
                             });
            }).chain(() -> {
                // Find the entity by ID
                return session.withTransaction(tx -> {
                    return new EntityClass()
                            .builder(session)
                            .find("test1")
                            .get()
                            .invoke(foundEntity -> {
                                // Verify the entity was found
                                assertNotNull(foundEntity);
                                assertEquals("test1", foundEntity.getId());
                                assertEquals("Test Entity", foundEntity.getName());
                                assertEquals("This is a test entity", foundEntity.getDescription());
                            });
                });
            });
        }).await().indefinitely();
    }

    @Test
    public void testEntityClassTwoPersistence()
    {
        // Create a parent entity
        EntityClass parent = new EntityClass()
                                     .setId("parent1")
                                     .setName("Parent Entity")
                                     .setDescription("This is a parent entity")
                ;

        // Use sessionFactory to start a reactive session and transaction
        sessionFactory.withSession(session -> {
            // Persist the parent entity
            return session.withTransaction(tx -> {
                return parent.builder(session)
                             .persist(parent);
            }).chain(persistedParent -> {
                // Create a child entity
                EntityClassTwo child = new EntityClassTwo()
                                       .setId("child1")
                                       .setName("Child Entity")
                                       .setValue(42)
                                       .setEntityClass(persistedParent)
                ;

                // Persist the child entity
                return session.withTransaction(tx -> {
                    return child.builder(session)
                                .persist(child)
                                .invoke(persistedChild -> {
                                    // Verify the child entity was persisted
                                    assertNotNull(persistedChild);
                                    assertEquals("child1", persistedChild.getId());
                                    assertEquals("Child Entity", persistedChild.getName());
                                    assertEquals(42, persistedChild.getValue());
                                    assertNotNull(persistedChild.getEntityClass());
                                    assertEquals("parent1", persistedChild.getEntityClass().getId());
                                });
                });
            });
        }).await().indefinitely();
    }

    @Test
    public void testCommonTableExpression()
    {
        // Seed three rows: two ACTIVE, one INACTIVE
        sessionFactory.withSession(session ->
                session.withTransaction(tx ->
                        new EntityClass().setId("cte1").setName("Alpha").setDescription("ACTIVE")
                                         .builder(session).persist()
                                         .chain(() -> new EntityClass().setId("cte2").setName("Avon").setDescription("ACTIVE")
                                                                       .builder(session).persist())
                                         .chain(() -> new EntityClass().setId("cte3").setName("Gamma").setDescription("ACTIVE")
                                                                       .builder(session).persist())
                                         .chain(() -> new EntityClass().setId("cte4").setName("Apex").setDescription("INACTIVE")
                                                                       .builder(session).persist())
                ).chain(() -> {
                    // CTE body: only ACTIVE rows
                    var activeOnly = new EntityClass().builder(session)
                                                      .where("description", Operand.Equals, "ACTIVE");

                    // Outer query reads FROM the CTE and filters names starting with "A"
                    return new EntityClass().builder(session)
                                            .with("active_entities", activeOnly)
                                            .where("name", Operand.Like, "A%")
                                            .getAll()
                                            .invoke(results -> {
                                                // Alpha + Avon match (ACTIVE and name A%); Gamma excluded by name; Apex excluded by description
                                                assertNotNull(results);
                                                assertEquals(2, results.size());
                                                assertTrue(results.stream().allMatch(e -> e.getName().startsWith("A")));
                                                assertTrue(results.stream().allMatch(e -> "ACTIVE".equals(e.getDescription())));
                                            });
                })
        ).await().indefinitely();
    }

    @Test
    public void testRecursiveHierarchyCommonTableExpression()
    {
        // Tree:  1 -> 2 -> 3, 1 -> 4   (subtree of 1 = {1,2,3,4})
        // Separate tree: 9 -> 10       (must NOT appear)
        sessionFactory.withSession(session ->
                session.withTransaction(tx ->
                        new CategoryNode().setId("1").setName("root").setParentId(null)
                                          .builder(session).persist()
                                          .chain(() -> new CategoryNode().setId("2").setName("child-of-1").setParentId("1")
                                                                         .builder(session).persist())
                                          .chain(() -> new CategoryNode().setId("3").setName("child-of-2").setParentId("2")
                                                                         .builder(session).persist())
                                          .chain(() -> new CategoryNode().setId("4").setName("child-of-1").setParentId("1")
                                                                         .builder(session).persist())
                                          .chain(() -> new CategoryNode().setId("9").setName("other-root").setParentId(null)
                                                                         .builder(session).persist())
                                          .chain(() -> new CategoryNode().setId("10").setName("child-of-9").setParentId("9")
                                                                         .builder(session).persist())
                ).chain(() -> {
                    // Anchor: start at node "1"
                    var anchor = new CategoryNode().builder(session)
                                                   .where("id", Operand.Equals, "1");

                    // Recursively walk children via the parentId self-reference
                    return new CategoryNode().builder(session)
                                             .withRecursiveHierarchy("subtree", anchor, "parentId")
                                             .getAll()
                                             .invoke(results -> {
                                                 assertNotNull(results);
                                                 var ids = results.stream().map(CategoryNode::getId).sorted().toList();
                                                 // node 1 and all of its descendants, nothing from the 9/10 tree
                                                 assertEquals(java.util.List.of("1", "2", "3", "4"), ids);
                                             });
                })
        ).await().indefinitely();
    }
}
