package com.entityassist.services.entities;

import com.entityassist.services.querybuilders.IQueryBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public interface IBaseEntity<J extends IBaseEntity<J, Q, I>, Q extends IQueryBuilder<Q, J, I>, I extends Serializable>
        extends IDefaultEntity<J, Q, I> {

}
