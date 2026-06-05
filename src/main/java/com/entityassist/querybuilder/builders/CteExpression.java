package com.entityassist.querybuilder.builders;

import com.entityassist.querybuilder.QueryBuilder;
import jakarta.persistence.criteria.AbstractQuery;
import org.hibernate.query.criteria.JpaCteCriteria;

import java.util.function.Function;

/**
 * Holds the definition of a single Common Table Expression (CTE) that is registered onto a
 * {@link org.hibernate.query.criteria.JpaCriteriaQuery} before execution.
 * <p>
 * Mirrors the role that {@link JoinExpression} plays for joins - it is a lightweight, mutable
 * carrier produced by the fluent {@code with(...)} / {@code withRecursive(...)} DSL on
 * {@link QueryBuilder} and resolved into a Hibernate {@link JpaCteCriteria} at build time.
 *
 * @param <T> The row type produced by the CTE
 */
@SuppressWarnings({"unused", "LombokGetterMayBeUsed"})
public final class CteExpression<T>
{
	/**
	 * The logical name assigned to the CTE (informational - Hibernate auto-generates the SQL alias).
	 */
	private String name;
	/**
	 * Whether this CTE is recursive.
	 */
	private boolean recursive;
	/**
	 * Whether a recursive CTE unions its members with UNION ALL (true) or UNION DISTINCT (false).
	 */
	private boolean unionAll = true;
	/**
	 * The builder that produces the (anchor) member query for this CTE.
	 */
	private QueryBuilder<?, ?, ?> definition;
	/**
	 * The producer for the recursive member of a recursive CTE. Receives the materialised
	 * {@link JpaCteCriteria} so the recursive query can reference the CTE itself.
	 */
	private Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveProducer;
	/**
	 * The materialised Hibernate CTE criteria, populated once the CTE is registered.
	 */
	private JpaCteCriteria<T> generatedCte;

	/**
	 * Creates an empty CTE expression.
	 */
	public CteExpression()
	{
		//No configuration required
	}

	/**
	 * Returns the logical name assigned to this CTE.
	 *
	 * @return The CTE name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the logical name for this CTE.
	 *
	 * @param name The CTE name
	 * @return This expression
	 */
	public CteExpression<T> setName(String name)
	{
		this.name = name;
		return this;
	}

	/**
	 * Returns whether this CTE is recursive.
	 *
	 * @return true when recursive
	 */
	public boolean isRecursive()
	{
		return recursive;
	}

	/**
	 * Sets whether this CTE is recursive.
	 *
	 * @param recursive true when recursive
	 * @return This expression
	 */
	public CteExpression<T> setRecursive(boolean recursive)
	{
		this.recursive = recursive;
		return this;
	}

	/**
	 * Returns whether recursive members are combined with UNION ALL.
	 *
	 * @return true for UNION ALL, false for UNION DISTINCT
	 */
	public boolean isUnionAll()
	{
		return unionAll;
	}

	/**
	 * Sets whether recursive members are combined with UNION ALL.
	 *
	 * @param unionAll true for UNION ALL, false for UNION DISTINCT
	 * @return This expression
	 */
	public CteExpression<T> setUnionAll(boolean unionAll)
	{
		this.unionAll = unionAll;
		return this;
	}

	/**
	 * Returns the builder that produces the (anchor) member query.
	 *
	 * @return The definition builder
	 */
	public QueryBuilder<?, ?, ?> getDefinition()
	{
		return definition;
	}

	/**
	 * Sets the builder that produces the (anchor) member query.
	 *
	 * @param definition The definition builder
	 * @return This expression
	 */
	public CteExpression<T> setDefinition(QueryBuilder<?, ?, ?> definition)
	{
		this.definition = definition;
		return this;
	}

	/**
	 * Returns the recursive member producer.
	 *
	 * @return The recursive producer, or {@code null} for non-recursive CTEs
	 */
	public Function<JpaCteCriteria<T>, AbstractQuery<T>> getRecursiveProducer()
	{
		return recursiveProducer;
	}

	/**
	 * Sets the recursive member producer.
	 *
	 * @param recursiveProducer The recursive producer
	 * @return This expression
	 */
	public CteExpression<T> setRecursiveProducer(Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveProducer)
	{
		this.recursiveProducer = recursiveProducer;
		return this;
	}

	/**
	 * Returns the materialized Hibernate CTE criteria.
	 *
	 * @return The generated CTE, or {@code null} until registered
	 */
	public JpaCteCriteria<T> getGeneratedCte()
	{
		return generatedCte;
	}

	/**
	 * Sets the materialized Hibernate CTE criteria.
	 *
	 * @param generatedCte The generated CTE
	 * @return This expression
	 */
	public CteExpression<T> setGeneratedCte(JpaCteCriteria<T> generatedCte)
	{
		this.generatedCte = generatedCte;
		return this;
	}
}

