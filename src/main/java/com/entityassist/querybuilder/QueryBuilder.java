package com.entityassist.querybuilder;

import com.entityassist.BaseEntity;
import com.entityassist.enumerations.OrderByType;
import com.entityassist.querybuilder.builders.CteExpression;
import com.entityassist.querybuilder.builders.DefaultQueryBuilder;
import com.entityassist.querybuilder.builders.JoinExpression;
import com.entityassist.services.querybuilders.IQueryBuilder;
import com.google.common.base.Strings;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.hibernate.FlushMode;
import org.hibernate.NonUniqueResultException;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.reactive.mutiny.Mutiny;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.entityassist.querybuilder.builders.IFilterExpression.isPluralOrMapAttribute;
import static com.entityassist.querybuilder.builders.IFilterExpression.isSingularAttribute;

/**
 * Concrete high-level query execution builder used by user-defined builder subclasses.
 * Extends the fluent DSL with result execution methods such as {@code get()}, {@code getAll()},
 * {@code getCount()}, {@code delete()}, and {@code truncate()}.
 *
 * @param <J> The concrete builder type (CRTP self-reference)
 * @param <E> The entity type
 * @param <I> The entity ID type
 */
@Log4j2
@SuppressWarnings({"unchecked", "unused"})
public abstract class QueryBuilder<J extends QueryBuilder<J, E, I>, E extends BaseEntity<E, J, I>, I extends Serializable>
        extends DefaultQueryBuilder<J, E, I>
        implements IQueryBuilder<J, E, I>
{
    /**
     * Marks if this query is selected
     */
    private boolean selected;
    /**
     * If the first result must be returned from a list
     */
    private boolean returnFirst;

    /**
     * Creates a query builder instance.
     */
    protected QueryBuilder()
    {
        // default constructor
    }

    /**
     * Trigger if select should happen
     *
     * @return if select should occur
     */
    @Override
    public boolean onSelect()
    {
        return true;
    }

    /**
     * Trigger events on the query when selects occur
     */
    @Override
    public void onSelectExecution(Mutiny.SelectionQuery<?> query)
    {
        //For inheritance
    }

    /**
     * Registers a non-recursive Common Table Expression (CTE) and constrains this entity query to the
     * rows produced by it, keeping the same fluent DSL.
     * <p>
     * The {@code definition} builder describes the CTE body (its {@code where(...)} filters). Its
     * identifier column becomes the CTE projection, and an outer {@code id IN (SELECT id FROM cte)}
     * predicate is added to this builder. The result is therefore a real, managed entity list - all
     * existing operations ({@code where}, {@code orderBy}, {@code groupBy}, {@code getAll},
     * {@code getCount}, projections) continue to work unchanged.
     * <p>
     * Generated SQL shape:
     * <pre>{@code WITH cte AS (SELECT e.id FROM entity e WHERE <definition>)
     * SELECT m.* FROM entity m WHERE m.id IN (SELECT cte.id FROM cte) AND <outer filters>}</pre>
     *
     * <pre>{@code
     * var activeOnly = new EntityClass().builder(session)
     *         .where("description", Operand.Equals, "ACTIVE");
     *
     * return new EntityClass().builder(session)
     *         .with("active_entities", activeOnly)
     *         .where("name", Operand.Like, "A%")
     *         .getAll();
     * }</pre>
     *
     * @param name       A logical name for the CTE (a unique one is generated when null/blank)
     * @param definition The builder that produces the CTE body
     * @return This builder, constrained by the CTE
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public J with(String name, QueryBuilder<?, E, ?> definition)
    {
        String cteName = resolveCteName(name);
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) getCriteriaBuilder();
        Field idField = findIdField();
        String idName = idField.getName();
        Class<Object> idType = (Class<Object>) idField.getType();
        String idAlias = cteName + "_id";

        // CTE body: SELECT e.<id> FROM entity e WHERE <definition filters>
        JpaCriteriaQuery<Object> definitionQuery = cb.createQuery(idType);
        JpaRoot<E> definitionRoot = definitionQuery.from(getEntityClass());
        definition.setCriteriaQuery(definitionQuery);
        definition.reset(definitionRoot);
        Path<Object> idSelection = definitionRoot.get(idName);
        idSelection.alias(idAlias);
        definitionQuery.select(idSelection);
        List<Predicate> definitionFilters = new ArrayList<>(definition.getFilters());
        definitionQuery.where(definitionFilters.toArray(new Predicate[0]));

        // Register the CTE on this (entity-rooted) query and add an id IN (SELECT id FROM cte) filter
        JpaCriteriaQuery<?> mainQuery = (JpaCriteriaQuery<?>) getCriteriaQuery();
        JpaCteCriteria<Object> cte = mainQuery.with(cteName, definitionQuery);
        applyCteMembership(mainQuery, cte, idName, idAlias, idType);

        CteExpression<Object> expression = new CteExpression<>()
                .setName(cteName)
                .setDefinition(definition)
                .setGeneratedCte(cte);
        getCtes().add(expression);
        return (J) this;
    }

    /**
     * Registers a recursive Common Table Expression (CTE) and constrains this entity query to the
     * identifiers it produces.
     * <p>
     * The {@code anchor} builder supplies the base member (projected to the entity identifier). The
     * {@code recursiveProducer} receives the materialised {@link JpaCteCriteria} so the recursive
     * member can reference the CTE itself - for example to walk a parent/child hierarchy. Members are
     * combined with {@code UNION ALL} when {@code unionAll} is {@code true}, otherwise
     * {@code UNION DISTINCT}. This is an advanced, lower-level entry point: the producer works directly
     * with the Hibernate criteria API.
     *
     * @param name              A logical name for the CTE (a unique one is generated when null/blank)
     * @param anchor            The builder that produces the anchor member
     * @param recursiveProducer Produces the recursive member, given the CTE self-reference
     * @param unionAll          {@code true} for UNION ALL, {@code false} for UNION DISTINCT
     * @return This builder, constrained by the recursive CTE
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public J withRecursive(String name,
                           QueryBuilder<?, E, ?> anchor,
                           Function<JpaCteCriteria<Object>, AbstractQuery<Object>> recursiveProducer,
                           boolean unionAll)
    {
        String cteName = resolveCteName(name);
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) getCriteriaBuilder();
        Field idField = findIdField();
        String idName = idField.getName();
        Class<Object> idType = (Class<Object>) idField.getType();
        String idAlias = cteName + "_id";

        // Anchor body: SELECT e.<id> FROM entity e WHERE <anchor filters>
        JpaCriteriaQuery<Object> anchorQuery = cb.createQuery(idType);
        JpaRoot<E> anchorRoot = anchorQuery.from(getEntityClass());
        anchor.setCriteriaQuery(anchorQuery);
        anchor.reset(anchorRoot);
        Path<Object> idSelection = anchorRoot.get(idName);
        idSelection.alias(idAlias);
        anchorQuery.select(idSelection);
        List<Predicate> anchorFilters = new ArrayList<>(anchor.getFilters());
        anchorQuery.where(anchorFilters.toArray(new Predicate[0]));

        JpaCriteriaQuery<?> mainQuery = (JpaCriteriaQuery<?>) getCriteriaQuery();
        JpaCteCriteria<Object> cte = unionAll
                ? mainQuery.withRecursiveUnionAll(cteName, anchorQuery, recursiveProducer)
                : mainQuery.withRecursiveUnionDistinct(cteName, anchorQuery, recursiveProducer);
        applyCteMembership(mainQuery, cte, idName, idAlias, idType);

        CteExpression<Object> expression = new CteExpression<>()
                .setName(cteName)
                .setRecursive(true)
                .setUnionAll(unionAll)
                .setDefinition(anchor)
                .setRecursiveProducer(recursiveProducer)
                .setGeneratedCte(cte);
        getCtes().add(expression);
        return (J) this;
    }

    /**
     * Adds the {@code <id> IN (SELECT <idAlias> FROM cte)} membership predicate against this builder's
     * root using a sub-query over the registered CTE.
     */
    @SuppressWarnings("unchecked")
    private void applyCteMembership(JpaCriteriaQuery<?> mainQuery,
                                    JpaCteCriteria<Object> cte,
                                    String idName,
                                    String idAlias,
                                    Class<Object> idType)
    {
        JpaSubQuery<Object> sub = (JpaSubQuery<Object>) mainQuery.subquery(idType);
        JpaRoot<Object> cteSubRoot = sub.from(cte);
        sub.select(cteSubRoot.get(idAlias));
        getFilters().add(getRoot().get(idName).in(sub));
    }

    /**
     * Registers a recursive Common Table Expression that walks an adjacency-list hierarchy and
     * constrains this query to every entity reachable from the anchor set.
     * <p>
     * This is the ergonomic, fully-managed recursive entry point. The {@code anchor} builder selects
     * the starting rows (for example the roots, or a single subtree root). The CTE then repeatedly
     * joins children to the working set using {@code childAttribute.equals(parentIdAttribute)} where
     * {@code parentAttribute} is the self-referencing attribute on the entity that holds the parent's
     * identifier value (a scalar FK column, dot-paths supported for associations such as
     * {@code "parent.id"}).
     * <p>
     * Generated SQL shape:
     * <pre>{@code WITH RECURSIVE tree(id) AS (
     *     SELECT e.id FROM entity e WHERE <anchor>          -- anchor member
     *     UNION ALL
     *     SELECT c.id FROM entity c, tree WHERE c.parent_id = tree.id  -- recursive member
     * )
     * SELECT m.* FROM entity m WHERE m.id IN (SELECT tree.id FROM tree)}</pre>
     *
     * <pre>{@code
     * var roots = new CategoryNode().builder(session)
     *         .where("id", Operand.Equals, "1");
     *
     * return new CategoryNode().builder(session)
     *         .withRecursiveHierarchy("subtree", roots, "parentId")
     *         .getAll();   // node 1 and all its descendants
     * }</pre>
     *
     * @param name            A logical name for the CTE (a unique one is generated when null/blank)
     * @param anchor          The builder selecting the anchor (starting) rows
     * @param parentAttribute The self-referencing attribute holding the parent identifier
     * @return This builder, constrained by the recursive hierarchy CTE
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public J withRecursiveHierarchy(String name, QueryBuilder<?, E, ?> anchor, String parentAttribute)
    {
        String cteName = resolveCteName(name);
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) getCriteriaBuilder();
        Field idField = findIdField();
        String idName = idField.getName();
        Class<Object> idType = (Class<Object>) idField.getType();
        String idAlias = cteName + "_id";
        Class<E> entityClass = getEntityClass();

        // Anchor member: SELECT e.<id> FROM entity e WHERE <anchor filters>
        JpaCriteriaQuery<Object> anchorQuery = cb.createQuery(idType);
        JpaRoot<E> anchorRoot = anchorQuery.from(entityClass);
        anchor.setCriteriaQuery(anchorQuery);
        anchor.reset(anchorRoot);
        Path<Object> anchorId = anchorRoot.get(idName);
        anchorId.alias(idAlias);
        anchorQuery.select(anchorId);
        anchorQuery.where(new ArrayList<>(anchor.getFilters()).toArray(new Predicate[0]));

        JpaCriteriaQuery<?> mainQuery = (JpaCriteriaQuery<?>) getCriteriaQuery();

        // Recursive member: SELECT c.<id> FROM entity c, cte WHERE c.<parent> = cte.<idAlias>
        Function<JpaCteCriteria<Object>, AbstractQuery<Object>> recursiveProducer = cteRef ->
        {
            JpaCriteriaQuery<Object> recursive = cb.createQuery(idType);
            JpaRoot<E> child = recursive.from(entityClass);
            JpaRoot<Object> parentRef = recursive.from(cteRef);
            Path<Object> childId = child.get(idName);
            childId.alias(idAlias);
            recursive.select(childId);
            recursive.where(cb.equal(traversePath(child, parentAttribute), parentRef.get(idAlias)));
            return recursive;
        };

        JpaCteCriteria<Object> cte = mainQuery.withRecursiveUnionAll(cteName, anchorQuery, recursiveProducer);
        applyCteMembership(mainQuery, cte, idName, idAlias, idType);

        CteExpression<Object> expression = new CteExpression<>()
                .setName(cteName)
                .setRecursive(true)
                .setUnionAll(true)
                .setDefinition(anchor)
                .setRecursiveProducer(recursiveProducer)
                .setGeneratedCte(cte);
        getCtes().add(expression);
        return (J) this;
    }

    /**
     * Traverses a dot-separated attribute path from the given root.
     *
     * @param root The starting from-node
     * @param path The dot-separated attribute path
     * @return The resolved path expression
     */
    private Path<Object> traversePath(From<?, ?> root, String path)
    {
        Path<?> current = root;
        for (String segment : path.split("\\."))
        {
            current = current.get(segment);
        }
        //noinspection unchecked
        return (Path<Object>) current;
    }

    /**
     * Locates the {@link jakarta.persistence.Id} annotated field on the entity hierarchy.
     *
     * @return The identifier field
     */
    private Field findIdField()
    {
        Class<?> current = getEntityClass();
        while (current != null && current != Object.class)
        {
            for (Field field : current.getDeclaredFields())
            {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class))
                {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        throw new com.entityassist.EntityAssistException("No @Id field found on " + getEntityClass().getName() + " - CTE support requires a single @Id field");
    }

    /**
     * Resolves the CTE name, generating a stable unique one when none is supplied.
     *
     * @param name The requested name, may be {@code null} or blank
     * @return A non-blank CTE name
     */
    private String resolveCteName(String name)
    {
        if (!Strings.isNullOrEmpty(name))
        {
            return name;
        }
        return "cte_" + Integer.toHexString(System.identityHashCode(this)) + "_" + getCtes().size();
    }

    /**
     * Returns a long of the count for the given builder
     *
     * @return Uni with a Long result, or a failure if onSelect() returns false
     */
    @Override
    public Uni<Long> getCount()
    {
        if (!selected)
        {
            selectCount();
            select();
        }
        if (onSelect())
        {
            Mutiny.SelectionQuery<Long> query = getQueryCount();
            applyCache(query);
            applyReadOnly(query);
            onSelectExecution(query);
            return query.getSingleResult();
        }
        return Uni.createFrom()
                       .failure(new NoResultException("No results found for the given criteria - onSelect() returned false"));
    }

    /**
     * Returns the generated query, always created new
     *
     * @param <T> Any type returned
     * @return A built typed query
     */
    @Override
    public <T> Mutiny.SelectionQuery<T> getQuery()
    {
        if (!selected)
        {
            select();
        }
        if(isStateless())
        {
            return getEntityManagerStateless().createQuery(getCriteriaQuery());
        }else
            return getEntityManager().createQuery(getCriteriaQuery());
    }

    /**
     * Returns the query for a count, always created new
     *
     * @param <T> Any type returned
     * @return A built typed query
     */
    @Override
    public <T> Mutiny.SelectionQuery<T> getQueryCount()
    {
        if (!selected)
        {
            selectCount();
            select();
        }
        if(isStateless())
        {
            return getEntityManagerStateless().createQuery(getCriteriaQuery());
        }else
            return getEntityManager().createQuery(getCriteriaQuery());
    }

    /**
     * Prepares the select statement
     *
     * @return This
     */
    @SuppressWarnings({"UnusedReturnValue"})
    @NotNull
    private J select()
    {
        if (!selected)
        {
            getJoins().forEach(this::processJoins);
            if (!isDelete() && !isUpdate())
            {
                processCriteriaQuery();
            }
            else if (isDelete())
            {
                CriteriaDelete<E> cq = getCriteriaDelete();
                List<Predicate> allWheres = new ArrayList<>(getFilters());
                Predicate[] preds = new Predicate[allWheres.size()];
                preds = allWheres.toArray(preds);
                cq.where(preds);
            }
            else if (isUpdate())
            {
                CriteriaUpdate<E> cq = getCriteriaUpdate();
                List<Predicate> allWheres = new ArrayList<>(getFilters());
                Predicate[] preds = new Predicate[allWheres.size()];
                preds = allWheres.toArray(preds);
                cq.where(preds);
            }
        }
        selected = true;
        return (J) this;
    }

    /**
     * Physically applies the cache attributes to the query
     * <p>
     * Adds cacheable, cache region, and sets persistence cache retrieve mode as use, and store mode as use
     *
     * @param query The query to apply to
     */
    private void applyCache(Mutiny.SelectionQuery<?> query)
    {
        if (!Strings.isNullOrEmpty(getCacheName()))
        {
            query.setCacheable(true);
            query.setCacheRegion(getCacheRegion());
            query.setCacheRetrieveMode(CacheRetrieveMode.USE);
            query.setCacheStoreMode(CacheStoreMode.USE);
        }
    }

    /**
     * Applies read-only tuning to the query when {@link #isReadOnly()} is enabled.
     * <p>
     * Read-only queries skip Hibernate's dirty-state snapshot retention and the pre-query
     * auto-flush ({@link FlushMode#MANUAL}), reducing CPU and GC for read-heavy paths such as
     * GraphQL data fetchers. Opt-in and additive — does nothing unless {@code setReadOnly(true)}
     * was called on the builder.
     *
     * @param query The query to apply to
     */
    private void applyReadOnly(Mutiny.SelectionQuery<?> query)
    {
        if (isReadOnly())
        {
            query.setReadOnly(true);
            query.setFlushMode(FlushMode.MANUAL);
        }
    }

    /**
     * Builds up the criteria query to perform (Criteria Query Only)
     */
    private void processCriteriaQuery()
    {
        CriteriaQuery<E> cq = getCriteriaQuery();
        List<Predicate> allWheres = new ArrayList<>(getFilters());
        Predicate[] preds = new Predicate[allWheres.size()];
        preds = allWheres.toArray(preds);
        getCriteriaQuery().where(preds);
        for (Expression<?> p : getGroupBys())
        {
            cq.groupBy(p);
        }

        for (Expression<?> expression : getHavingExpressions())
        {
            cq.having((Expression<Boolean>) expression);
        }

        if (!getOrderBys().isEmpty())
        {
            List<Order> orderBys = new ArrayList<>();
            getOrderBys().forEach((key, value) ->
                                          orderBys.add(processOrderBys(key, value)));
            cq.orderBy(orderBys);
        }

        if (getSelections().isEmpty())
        {
            getCriteriaQuery().select(getRoot());
        }
        else if (getSelections().size() > 1)
        {
            if (getConstruct() != null)
            {
                ArrayList<Selection<?>> aS = new ArrayList<>(getSelections());
                Selection<?>[] selections = aS.toArray(new Selection[0]);
                CompoundSelection<?> cs = getCriteriaBuilder().construct(getConstruct(), selections);
                getCriteriaQuery().select(cs);
            }
            else
            {
                getCriteriaQuery().multiselect(new ArrayList<>(getSelections()));
            }
        }
        else
        {
            getSelections().forEach(a -> getCriteriaQuery().select(a));
        }
    }

    /**
     * Processes the order bys into the given query
     *
     * @param key   The attribute to apply
     * @param value The value to use
     */
    private Order processOrderBys(Attribute<?, ?> key, OrderByType value)
    {
        //noinspection EnhancedSwitchMigration
        switch (value)
        {

            case DESC:
            {
                if (isSingularAttribute(key))
                {
                    return getCriteriaBuilder().desc(getRoot().get((SingularAttribute<?, ?>) key));
                }
                else if (isPluralOrMapAttribute(key))
                {
                    return getCriteriaBuilder().desc(getRoot().get((PluralAttribute<?, ?, ?>) key));
                }
                return getCriteriaBuilder().desc(getRoot().get((SingularAttribute<?, ?>) key));
            }
            case ASC:
            default:
            {
                if (isSingularAttribute(key))
                {
                    return getCriteriaBuilder().asc(getRoot().get((SingularAttribute<?, ?>) key));
                }
                else if (isPluralOrMapAttribute(key))
                {
                    return getCriteriaBuilder().asc(getRoot().get((PluralAttribute<?, ?, ?>) key));
                }
                return getCriteriaBuilder().asc(getRoot().get((SingularAttribute<?, ?>) key));
            }
        }
    }

    /**
     * Processors the join section
     *
     * @param executor Processes the joins into the expression
     */
    private void processJoins(JoinExpression<?, ?, ?> executor)
    {
        Attribute<?, ?> value = executor.getAttribute();
        JoinType jt = executor.getJoinType();
        List<Predicate> onClause = new ArrayList<>();
        if (executor.getOnBuilder() != null)
        {
            executor.getOnBuilder()
                    .select();
            onClause.addAll(executor.getOnBuilder()
                                    .getFilters());
        }

        Join<?, ?> join;
        if (executor.getGeneratedRoot() == null)
        {
            join = getRoot().join(value.getName(), jt);
        }
        else
        {
            //join = getRoot().join(value.getName(), jt);
            join = executor.getGeneratedRoot();
        }
        if (!onClause.isEmpty())
        {
            join = join.on(onClause.toArray(new Predicate[]{}));
        }
        QueryBuilder<?, ?, ?> key = executor.getExecutor();
        if (key != null)
        {
            key.reset(join);
            key.select();
            getSelections().addAll(key.getSelections());
            getFilters().addAll(key.getFilters());
            getOrderBys().putAll(key.getOrderBys());
        }
    }

    /**
     * Returns the result set as a stream
     *
     * @param resultType The result type
     * @param <T>        The Class for the type to gerenify
     * @return Uni with a list of results, or a failure if onSelect() returns false
     */
    @Override
    @SuppressWarnings({"Duplicates", "unused"})
    public <T> Uni<List<T>> getResultStream(Class<T> resultType)
    {
        if (!selected)
        {
            select();
        }
        if (onSelect())
        {
            Mutiny.SelectionQuery<T> query = getQuery();
            applyCache(query);
            applyReadOnly(query);
            if (getMaxResults() != null)
            {
                query.setMaxResults(getMaxResults());
            }
            if (getFirstResults() != null)
            {
                query.setFirstResult(getFirstResults());
            }
            onSelectExecution(query);
            return query.getResultList();
        }
        return Uni.createFrom()
                       .failure(new NoResultException("No results found for the given criteria - onSelect() returned false"));
    }

    /**
     * Returns a non-distinct list and returns an empty optional if a non-unique-result exception is thrown
     *
     * @return An optional of the result
     */
    @Override
    public Uni<E> get()
    {
        return get(this.returnFirst);
    }

    /**
     * Returns the first result returned
     *
     * @param returnFirst If the first should be returned in the instance of many results
     * @return Optional of the required object
     */
    @Override
    @NotNull
    public Uni<E> get(boolean returnFirst)
    {
        this.returnFirst = returnFirst;
        return get(getEntityClass());
    }

    /**
     * Returns the runtime builder class.
     *
     * @return The concrete builder class
     */
    public Class<J> getMeClass()
    {
        return (Class<J>) getClass();
    }

    /**
     * Returns a list (distinct or not) and returns an empty optional if returns a list, or will simply return the first result found from
     * a list with the same criteria
     *
     * @return Optional of the given class type (which should be a select column)
     */
    @Override
    @SuppressWarnings({"unused"})
    @NotNull
    public <T> Uni<T> get(@NotNull Class<T> asType)
    {
        if (!selected)
        {
            select();
        }
        if (onSelect())
        {
            Mutiny.SelectionQuery<T> query = getQuery();
            if (getMaxResults() != null)
            {
                query.setMaxResults(getMaxResults());
            }else {
              query.setMaxResults(1);
            }
            if (getFirstResults() != null)
            {
                query.setFirstResult(getFirstResults());
            }
            applyCache(query);
            applyReadOnly(query);
            onSelectExecution(query);
            Uni<T> j;
                return query.getSingleResult()
                           .onFailure(NonUniqueResultException.class)
                           .invoke(a->log.fatal("getSingle instead of getAll, or filters not correct getSingleResult - " + getEntityClass().getCanonicalName() + " - " + getMeClass(),a))
                           .invoke(res -> {
                               if(res instanceof BaseEntity)
                                ((BaseEntity<?, ?, ?>) res).setFake(false);
                           });
        }
        return Uni.createFrom()
                       .failure(new NoResultException("No results found for the given criteria"));
    }

    /**
     * If this builder is configured to return the first row
     *
     * @return If the first record must be returned
     */
    @Override
    @SuppressWarnings("WeakerAccess")
    public boolean isReturnFirst()
    {
        return returnFirst;
    }

    /**
     * If a Non-Unique Exception is thrown re-run the query as a list and return the first item
     *
     * @param returnFirst if must return first
     * @return J
     */
    @Override
    @SuppressWarnings({"unchecked", "unused"})
    @NotNull
    public @org.jspecify.annotations.NonNull J setReturnFirst(boolean returnFirst)
    {
        this.returnFirst = returnFirst;
        return (J) this;
    }

    /**
     * Returns a list of entities from a distinct or non distinct list
     *
     * @return A list of entities returned
     */
    @Override
    public Uni<List<E>> getAll()
    {
        return getAll(getEntityClass());
    }

    /**
     * Returns the list as the selected class type (for when specifying single select columns)
     *
     * @param returnClassType Returns a list of a given column
     * @param <T>             The type of the column returned
     * @return Uni with a list of the given column type, or a failure if onSelect() returns false
     */
    @Override
    @SuppressWarnings({"Duplicates", "unused"})
    @NotNull
    public <T> Uni<List<T>> getAll(Class<T> returnClassType)
    {
        if (!selected)
        {
            select();
        }
        if (onSelect())
        {
            Mutiny.SelectionQuery<T> query = getQuery();
            applyCache(query);
            applyReadOnly(query);
            if (getMaxResults() != null)
            {
                query.setMaxResults(getMaxResults());
            }
            if (getFirstResults() != null)
            {
                query.setFirstResult(getFirstResults());
            }
            onSelectExecution(query);
            return query.getResultList().invoke(res -> {
                res.forEach(e -> ((BaseEntity<?, ?, ?>) e).setFake(false));
            });
        }
        return Uni.createFrom()
                       .failure(new NoResultException("Query could not run for the given criteria - onSelect() returned false"));
    }

    /**
     * Returns the number of rows affected by the delete.
     * <p>
     * Bulk Delete Operation
     * <p>
     * WARNING : Be very careful if you haven't added a filter this will truncate the table or throw a unsupported exception if no filters.
     *
     * @return number of results deleted
     */
    @Override
    public Uni<Integer> delete()
    {
        if (getFilters().isEmpty())
        {
            throw new UnsupportedOperationException("Calling the delete method with no filters. This will truncate the table. Rather call truncate()");
        }
        CriteriaDelete<E> deletion = getCriteriaBuilder().createCriteriaDelete(getEntityClass());
        reset(deletion.from(getEntityClass()));
        setCriteriaDelete(deletion);
        select();
        if(isStateless())
        {
            return getEntityManagerStateless().createQuery(deletion)
                       .executeUpdate();
        }else
            return getEntityManager().createQuery(deletion)
                       .executeUpdate();
    }

    /**
     * Deletes the given entity through the entity manager
     *
     * @param entity Deletes through the entity manager
     * @return This
     */
    @Override
    public Uni<E> delete(E entity)
    {
        if (isStateless())
        {
            return getEntityManagerStateless().delete(entity).map(_ -> entity);
        }
        return getEntityManager().remove(entity)
                       .map(_ -> entity);
    }


    /**
     * Returns the number of rows affected by the delete.
     * WARNING : Be very careful if you haven't added a filter this will truncate the table or throw a unsupported exception if no filters.
     *
     * @return The number of records deleted
     */
    @Override
    @SuppressWarnings({"unused"})
    public Uni<Integer> truncate()
    {
        CriteriaDelete<E> deletion = getCriteriaBuilder().createCriteriaDelete(getEntityClass());
        setCriteriaDelete(deletion);
        reset(deletion.from(getEntityClass()));
        getFilters().clear();
        select();
        if(isStateless())
        {
            return getEntityManagerStateless().createQuery(deletion)
                       .executeUpdate();
        }else
            return getEntityManager().createQuery(deletion)
                       .executeUpdate();
    }
}
