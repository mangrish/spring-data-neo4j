/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.events;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;
import org.springframework.data.neo4j.event.Neo4jDataEvent;

/**
 * Spring {@code ApplicationListener} used to capture {@link Neo4jDataEvent}s that occur during a test run.
 * Note that this is abstract because you're supposed to create an anonymous subclass to handle event type 'E' when you
 * use it.  This ensures Spring doesn't just send {@link Neo4jDataEvent}s to everything regardless.
 *
 * @author Adam George
 */
public abstract class TestNeo4jEventListener<E extends Neo4jDataEvent> implements ApplicationListener<E> {

    private List<Neo4jDataEvent> events = new ArrayList();

    @Override
    public void onApplicationEvent(E event) {
        this.events.add(event);
    }

    public boolean hasReceivedAnEvent() {
        return this.events.size() > 0;
    }

    public List<Neo4jDataEvent> getEvents() {
        return events;
    }

    public void clear() {
        events.clear();
    }

    public boolean hasObject(Object object) {
        for (Neo4jDataEvent event : events) {
            if (event.getEntity() == object)
                return true;
        }
        return false;
    }

}
