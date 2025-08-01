package com.entityassist.querybuilder.builders;

import com.entityassist.BaseEntity;
import com.entityassist.enumerations.Operand;
import com.entityassist.querybuilder.QueryBuilder;

import com.guicedee.client.IGuiceContext;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Collection;

/**
 * Public join expression
 */
@SuppressWarnings("unused")
public final class JoinExpression<X extends BaseEntity<X, ?, ?>, Y, Z>
{
  /**
   * The query build that will run the execution for the join
   */
  private QueryBuilder<?, X, ?> executor;
  /**
   * The given join type
   */
  private JoinType joinType;
  /**
   * The attribute type to apply
   */
  private Attribute<X, Y> attribute;
  /**
   * The builder to use for the on clause
   */
  private QueryBuilder<?, X, ?> onBuilder;
  /**
   * The generated root from the join
   */
  private Join<X, Y> generatedRoot;

  /**
   * The join expression to build
   */
  public JoinExpression()
  {
    //No config required
  }

  /**
   * A new expression with the given configurations
   *
   * @param executor  The Query Builder to use
   * @param joinType  The join type to apply
   * @param attribute The attribute to apply it with
   */
  @SuppressWarnings("WeakerAccess")
  public JoinExpression(QueryBuilder<?, X, ?> executor, JoinType joinType, Attribute<X, Y> attribute)
  {
    this.executor = executor;
    this.joinType = joinType;
    this.attribute = attribute;
  }

  @SuppressWarnings("WeakerAccess")
  public JoinExpression(QueryBuilder<?, X, ?> executor, JoinType joinType, Attribute<X, Y> attribute, QueryBuilder<?, X, ?> onBuilder)
  {
    this.executor = executor;
    this.joinType = joinType;
    this.attribute = attribute;
    this.onBuilder = onBuilder;
  }

  /**
   * Method getExecutor returns the executor of this JoinExpression object.
   * <p>
   * The query build that will run the execution for the join
   *
   * @return the executor (type QueryBuilder ?, X, ? ) of this JoinExpression object.
   */
  public QueryBuilder<?, X, ?> getExecutor()
  {
    return executor;
  }

  /**
   * Method setExecutor sets the executor of this JoinExpression object.
   * <p>
   * The query build that will run the execution for the join
   *
   * @param executor the executor of this JoinExpression object.
   */
  public void setExecutor(QueryBuilder<?, X, ?> executor)
  {
    this.executor = executor;
  }

  /**
   * Method getJoinType returns the joinType of this JoinExpression object.
   * <p>
   * The given join type
   *
   * @return the joinType (type JoinType) of this JoinExpression object.
   */
  public JoinType getJoinType()
  {
    return joinType;
  }

  /**
   * Method setJoinType sets the joinType of this JoinExpression object.
   * <p>
   * The given join type
   *
   * @param joinType the joinType of this JoinExpression object.
   */
  public void setJoinType(JoinType joinType)
  {
    this.joinType = joinType;
  }

  /**
   * Method getAttribute returns the attribute of this JoinExpression object.
   * <p>
   * The attribute type to apply
   *
   * @return the attribute (type Attribute X, Y) of this JoinExpression object.
   */
  public Attribute<X, Y> getAttribute()
  {
    return attribute;
  }

  /**
   * Method setAttribute sets the attribute of this JoinExpression object.
   * <p>
   * The attribute type to apply
   *
   * @param attribute the attribute of this JoinExpression object.
   */
  public void setAttribute(Attribute<X, Y> attribute)
  {
    this.attribute = attribute;
  }

  /**
   * Getter for property 'onBuilder'.
   *
   * @return Value for property 'onBuilder'.
   */
  public QueryBuilder<?, X, ?> getOnBuilder()
  {
    return onBuilder;
  }

  /**
   * Setter for property 'onBuilder'.
   *
   * @param onBuilder Value to set for property 'onBuilder'.
   */
  public void setOnBuilder(QueryBuilder<?, X, ?> onBuilder)
  {
    this.onBuilder = onBuilder;
  }

  /**
   * The generated root if any
   *
   * @return
   */
  public Join<X, Y> getGeneratedRoot()
  {
    return generatedRoot;
  }

  /**
   * The generated root
   *
   * @param generatedRoot
   * @return
   */
  public JoinExpression<X, Y, Z> setGeneratedRoot(Join<X, Y> generatedRoot)
  {
    this.generatedRoot = generatedRoot;
    return this;
  }

  public Predicate getFilter(String fieldName, Operand operand, Object value)
  {
    Join joinRoot = getGeneratedRoot();
    Path<?> path = joinRoot.get(fieldName);
    HibernateCriteriaBuilder cb = IGuiceContext.get(Mutiny.SessionFactory.class)
                                      .getCriteriaBuilder();
    return switch (operand)
    {
      case InList -> path.in((Collection<?>) value);
      case NotInList -> path.in((Collection<?>) value)
                            .not();

      case Equals ->
      {
        if (value == null)
        {
          yield cb.isNull(path);
        }
        if (value instanceof Collection<?> col)
        {
          yield path.in((Collection<?>) col);
        }
        yield cb.equal(path, value);
      }

      case NotEquals ->
      {
        if (value == null)
        {
          yield cb.isNotNull(path);
        }
        if (value instanceof Collection<?> col)
        {
          yield path.in(col)
                    .not(); // 👈
        }
        yield cb.notEqual(path, value);
      }

      case Like -> cb.like(path.as(String.class), value.toString());
      case NotLike -> cb.notLike(path.as(String.class), value.toString());

      case Null -> cb.isNull(path);
      case NotNull -> cb.isNotNull(path);

      case GreaterThan, GreaterThanEqualTo, LessThan, LessThanEqualTo -> handleComparableOperand(cb, path, operand, value);
    };
  }


  @SuppressWarnings("unchecked")
  private <T extends Comparable<? super T>> Predicate handleComparableOperand(
      HibernateCriteriaBuilder cb,
      Path<?> path,
      Operand operand,
      Object value
  )
  {
    Path<T> typedPath = (Path<T>) path;
    T typedValue = (T) value;

    return switch (operand)
    {
      case GreaterThan -> cb.greaterThan(typedPath, typedValue);
      case GreaterThanEqualTo -> cb.greaterThanOrEqualTo(typedPath, typedValue);
      case LessThan -> cb.lessThan(typedPath, typedValue);
      case LessThanEqualTo -> cb.lessThanOrEqualTo(typedPath, typedValue);
      default -> throw new UnsupportedOperationException("Operand not supported here: " + operand);
    };
  }

}
