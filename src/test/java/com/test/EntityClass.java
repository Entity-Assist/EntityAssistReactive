package com.test;

import com.entityassist.BaseEntity;
import com.entityassist.querybuilder.QueryBuilder;
import com.guicedee.client.IGuiceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.reactive.mutiny.Mutiny;

@Entity
@Accessors(chain = true)
@Table(name = "entity_class")
public class EntityClass extends BaseEntity<EntityClass, EntityClass.EntityClassQueryBuilder, String>
{

    @Id
    @Column(name = "id", nullable = false)
    @Getter
    @Setter
    private String id;

    @Column(name = "name")
    @Getter
    @Setter
    private String name;

    @Column(name = "description")
    @Getter
    @Setter
    private String description;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public EntityClass setId(String id) {
        this.id = id;
        return this;
    }

    public static class EntityClassQueryBuilder extends QueryBuilder<EntityClassQueryBuilder, EntityClass, String>
    {
        public EntityClassQueryBuilder() {
            super();
        }

        @Override
        public boolean isIdGenerated() {
            return false;
        }
    }
}
