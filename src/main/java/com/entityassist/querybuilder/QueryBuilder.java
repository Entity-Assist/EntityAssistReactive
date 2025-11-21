package com.entityassist.querybuilder;

import com.entityassist.BaseEntity;
import com.entityassist.enumerations.OrderByType;
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
import org.hibernate.NonUniqueResultException;
import org.hibernate.reactive.mutiny.Mutiny;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.entityassist.querybuilder.builders.IFilterExpression.isPluralOrMapAttribute;
import static com.entityassist.querybuilder.builders.IFilterExpression.isSingularAttribute;

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
    @SuppressWarnings({"UnusedReturnValue", "Duplicates"})
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
    @SuppressWarnings({"Duplicates", "unused"})
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
    @SuppressWarnings("Duplicates")
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
    @SuppressWarnings({"unused", "Duplicates"})
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
