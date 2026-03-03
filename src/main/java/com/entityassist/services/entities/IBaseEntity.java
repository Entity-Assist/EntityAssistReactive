package com.entityassist.services.entities;

import com.entityassist.services.querybuilders.IQueryBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * SPI contract for the primary entity superclass.
 * Extends {@link IDefaultEntity} and binds to {@link IQueryBuilder} for full CRUD and query support.
 *
 * @param <J> The concrete entity type (CRTP self-reference)
 * @param <Q> The associated query builder type
 * @param <I> The entity ID type
 */
public interface IBaseEntity<J extends IBaseEntity<J, Q, I>, Q extends IQueryBuilder<Q, J, I>, I extends Serializable>
        extends IDefaultEntity<J, Q, I> {

}
