package com.test;

import com.entityassist.BaseEntity;
import com.entityassist.querybuilder.QueryBuilder;
import com.guicedee.client.IGuiceContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.reactive.mutiny.Mutiny;

@Entity
@Accessors(chain = true)
@Table(name = "entity_class_two")
public class EntityClassTwo extends BaseEntity<EntityClassTwo, EntityClassTwo.EntityClassTwoQueryBuilder, String>
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

    @Column(name = "value")
    @Getter
    @Setter
    private Integer value;

    @ManyToOne
    @JoinColumn(name = "entity_class_id")
    @Getter
    @Setter
    private EntityClass entityClass;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public EntityClassTwo setId(String id) {
        this.id = id;
        return this;
    }

    public static class EntityClassTwoQueryBuilder extends QueryBuilder<EntityClassTwoQueryBuilder, EntityClassTwo, String>
    {
        public EntityClassTwoQueryBuilder() {
            // Initialize collections
            super();
        }

        @Override
        public boolean isIdGenerated() {
            return false;
        }
    }
}
