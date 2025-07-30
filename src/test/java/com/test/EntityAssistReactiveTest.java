package com.test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertxpersistence.PersistService;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import io.smallrye.mutiny.Uni;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
