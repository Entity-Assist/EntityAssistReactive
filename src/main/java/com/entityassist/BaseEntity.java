package com.entityassist;

import com.entityassist.querybuilder.QueryBuilder;
import com.entityassist.services.entities.IBaseEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@MappedSuperclass()
@JsonAutoDetect(fieldVisibility = ANY,
        getterVisibility = NONE,
        setterVisibility = NONE)
@JsonInclude(NON_NULL)
public abstract class BaseEntity<J extends BaseEntity<J, Q, I>, Q extends QueryBuilder<Q, J, I>, I extends Serializable>
        extends DefaultEntity<J, Q, I>
        implements IBaseEntity<J, Q, I>
{
    private static final Logger log = Logger.getLogger(BaseEntity.class.getName());

    /**
     * Constructs a new base entity type
     */
    public BaseEntity()
    {
        //No configuration needed
        setFake(true);
    }

}
