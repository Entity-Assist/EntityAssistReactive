package com.entityassist.services.entities;

import com.entityassist.services.querybuilders.IDefaultQueryBuilder;
import com.google.common.base.Strings;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * Intermediate SPI contract between {@link IRootEntity} and {@link IBaseEntity}.
 * Reserved for shared entity behavior that applies to the default entity tier.
 *
 * @param <J> The concrete entity type (CRTP self-reference)
 * @param <Q> The concrete query builder type
 * @param <I> The identifier type
 */
public interface IDefaultEntity<J extends IDefaultEntity<J, Q, I>, Q extends IDefaultQueryBuilder<Q, J, I>, I extends Serializable>
        extends IRootEntity<J, Q, I> {
	
	
}
