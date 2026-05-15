package com.entityassist;

import com.entityassist.querybuilder.builders.QueryBuilderRoot;
import org.hibernate.reactive.mutiny.Mutiny;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Static entry point for creating typed query builders from entity classes.
 */
public final class EA
{
    private EA()
    {
        // utility class
    }

    /**
     * Creates a typed entry point from an entity class.
     *
     * @param entityClass The entity class to instantiate
     * @param <E> The entity type
     * @param <Q> The query builder type
     * @param <I> The identifier type
     * @return A fluent static initializer for query builder access
     */
    public static <E extends RootEntity<E, Q, I>, Q extends QueryBuilderRoot<Q, E, I>, I extends Serializable> From<E, Q, I> from(Class<E> entityClass)
    {
        return new From<>(entityClass, null);
    }

    /**
     * Creates a typed entry point from an existing entity instance.
     *
     * @param entity The entity instance to bind
     * @param <E> The entity type
     * @param <Q> The query builder type
     * @param <I> The identifier type
     * @return A fluent static initializer for query builder access
     */
    public static <E extends RootEntity<E, Q, I>, Q extends QueryBuilderRoot<Q, E, I>, I extends Serializable> From<E, Q, I> from(E entity)
    {
        Class<E> entityClass = resolveEntityClass(entity);
        return new From<>(entityClass, entity);
    }

    @SuppressWarnings("unchecked")
    private static <E extends RootEntity<E, Q, I>, Q extends QueryBuilderRoot<Q, E, I>, I extends Serializable> Class<E> resolveEntityClass(E entity)
    {
        return (Class<E>) entity.getClass();
    }

    /**
     * Typed fluent initializer used by {@link EA#from(Class)} and {@link EA#from(RootEntity)}.
     */
    public static final class From<E extends RootEntity<E, Q, I>, Q extends QueryBuilderRoot<Q, E, I>, I extends Serializable>
    {
        private final Class<E> entityClass;
        private final E entity;
        private Mutiny.Session session;
        private Mutiny.StatelessSession statelessSession;

        private From(Class<E> entityClass, E entity)
        {
            this.entityClass = Objects.requireNonNull(entityClass, "entityClass may not be null");
            this.entity = entity;
        }

        /**
         * Binds the builder to a reactive stateful session.
         *
         * @param session The mutiny session to use
         * @return This initializer
         */
        public From<E, Q, I> withSession(Mutiny.Session session)
        {
            this.session = Objects.requireNonNull(session, "session may not be null");
            this.statelessSession = null;
            return this;
        }

        /**
         * Binds the builder to a reactive stateless session.
         *
         * @param statelessSession The mutiny stateless session to use
         * @return This initializer
         */
        public From<E, Q, I> withSession(Mutiny.StatelessSession statelessSession)
        {
            this.statelessSession = Objects.requireNonNull(statelessSession, "statelessSession may not be null");
            this.session = null;
            return this;
        }

        /**
         * Applies query-builder options and returns the typed builder.
         *
         * @return The configured builder
         */
        public Q withQueryBuilderOptions()
        {
            return createBuilder();
        }

        /**
         * Applies query-builder options and returns the typed builder.
         *
         * @param queryBuilderOptions Additional builder configuration callback
         * @return The configured builder
         */
        public Q withQueryBuilderOptions(Consumer<Q> queryBuilderOptions)
        {
            Q builder = createBuilder();
            queryBuilderOptions.accept(builder);
            return builder;
        }

        private Q createBuilder()
        {
            E resolvedEntity = entity != null ? entity : instantiateEntity();
            if (session != null)
            {
                return resolvedEntity.builder(session);
            }
            if (statelessSession != null)
            {
                return resolvedEntity.builder(statelessSession);
            }
            throw new EntityAssistException("No session is configured. Call withSession(...) before withQueryBuilderOptions().");
        }

        private E instantiateEntity()
        {
            try
            {
                Constructor<E> constructor = entityClass.getDeclaredConstructor();
                if (!constructor.canAccess(null))
                {
                    constructor.setAccessible(true);
                }
                return constructor.newInstance();
            }
            catch (Exception e)
            {
                throw new EntityAssistException("Unable to instantiate entity " + entityClass.getName() + ". Ensure a no-arg constructor exists.", e);
            }
        }
    }
}


