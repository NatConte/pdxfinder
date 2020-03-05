package org.pdxfinder.graph.dao;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.neo4j.ogm.annotation.NodeEntity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Represents the type of implantation. e.g. Orthotopic, Heterotopic
 */
@NodeEntity
public class EngraftmentType {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private EngraftmentType() {
        // Empty constructor required as of Neo4j API 2.0.5
    }

    public EngraftmentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EngraftmentType that = (EngraftmentType) o;

        return new EqualsBuilder()
            .append(getName(), that.getName())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getName())
            .toHashCode();
    }
}
