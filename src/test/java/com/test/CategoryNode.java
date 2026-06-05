package com.test;

import com.entityassist.BaseEntity;
import com.entityassist.querybuilder.QueryBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Self-referential adjacency-list entity used to exercise recursive CTE support.
 * Each node optionally points at its parent via the scalar {@code parentId} column.
 */
@Entity
@Accessors(chain = true)
@Table(name = "category_node")
public class CategoryNode extends BaseEntity<CategoryNode, CategoryNode.CategoryNodeQueryBuilder, String>
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

    @Column(name = "parent_id")
    @Getter
    @Setter
    private String parentId;

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public CategoryNode setId(String id)
    {
        this.id = id;
        return this;
    }

    public static class CategoryNodeQueryBuilder extends QueryBuilder<CategoryNodeQueryBuilder, CategoryNode, String>
    {
        public CategoryNodeQueryBuilder()
        {
            super();
        }

        @Override
        public boolean isIdGenerated()
        {
            return false;
        }
    }
}

