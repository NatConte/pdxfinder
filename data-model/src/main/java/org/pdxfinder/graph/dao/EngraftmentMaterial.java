package org.pdxfinder.graph.dao;

import org.neo4j.ogm.annotation.NodeEntity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/*
 * Created by csaba on 26/04/2018.
 */
@NodeEntity
public class EngraftmentMaterial {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String state;


    public EngraftmentMaterial() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
