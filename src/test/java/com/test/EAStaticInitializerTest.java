package com.test;

import com.entityassist.EA;
import com.entityassist.EntityAssistException;
import com.guicedee.client.IGuiceContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.hibernate.reactive.mutiny.Mutiny;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EAStaticInitializerTest
{
    private static final Mutiny.Session NO_OP_SESSION = (Mutiny.Session) Proxy.newProxyInstance(
            Mutiny.Session.class.getClassLoader(),
            new Class[]{Mutiny.Session.class},
            (proxyInstance, method, arguments) -> {
                switch (method.getName())
                {
                    case "toString":
                        return "NO_OP_SESSION";
                    case "hashCode":
                        return System.identityHashCode(proxyInstance);
                    case "equals":
                        return proxyInstance == arguments[0];
                    default:
                        return null;
                }
            }
    );

    @BeforeAll
    static void initContext()
    {
        IGuiceContext.instance();
    }

    @Test
    void fromClassCreatesTypedBuilderAndAppliesOptions()
    {
        var builder = EA.from(EntityClass.class)
                .withSession(NO_OP_SESSION)
                .withQueryBuilderOptions(qb -> qb.setMaxResults(7));

        assertNotNull(builder);
        assertEquals(EntityClass.class, builder.getEntityClass());
        assertEquals(7, builder.getMaxResults());
    }

    @Test
    void fromEntityCreatesTypedBuilder()
    {
        var builder = EA.from(new EntityClass())
                .withSession(NO_OP_SESSION)
                .withQueryBuilderOptions();

        assertNotNull(builder);
        assertEquals(EntityClass.class, builder.getEntityClass());
    }

    @Test
    void requiresSessionBeforeWithQueryBuilderOptions()
    {
        assertThrows(EntityAssistException.class, () -> EA.from(EntityClass.class).withQueryBuilderOptions());
    }
}


