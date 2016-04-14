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

package org.springframework.data.neo4j.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.events.TestNeo4jEventListener;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
/**
 * Test to assert the behaviour of {@link Neo4jTemplate}'s interaction with Spring application events.
 * @author Adam George
 */
@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateApplicationEventTest extends MultiDriverTestClass {

    @Autowired
    private Neo4jOperations neo4jTemplate;

    @Autowired
    private TestNeo4jEventListener<BeforeSaveEvent> beforeSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterSaveEvent> afterSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<BeforeDeleteEvent> beforeDeleteEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterDeleteEvent> afterDeleteEventListener;

    @Before
    public void initialiseEventListeners() {
        beforeSaveEventListener.clear();
        afterSaveEventListener.clear();
        beforeDeleteEventListener.clear();
        afterDeleteEventListener.clear();
    }

    @Test
    public void shouldCreateTemplateAndPublishAppropriateApplicationEventsOnSaveAndOnDelete() {
        assertNotNull("The Neo4jTemplate wasn't autowired into this test", this.neo4jTemplate);

        Actor entity = new Actor();
        entity.setName("John Abraham");

        assertFalse(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertFalse(this.afterSaveEventListener.hasReceivedAnEvent());
        this.neo4jTemplate.save(entity);
        assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeSaveEventListener.getEvents().get(0).getEntity());
        assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterSaveEventListener.getEvents().get(0).getEntity());

        assertFalse(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertFalse(this.afterDeleteEventListener.hasReceivedAnEvent());

        this.neo4jTemplate.delete(entity);
        assertTrue(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeDeleteEventListener.getEvents().get(0).getEntity());
        assertTrue(this.afterDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterDeleteEventListener.getEvents().get(0).getEntity());
    }

    @Test
    public void shouldCreateTemplateAndPublishAppropriateApplicationEventsForNestedObject() {
        assertNotNull("The Neo4jTemplate wasn't autowired into this test", this.neo4jTemplate);

        User user = new User();
        Cinema cinema = new Cinema("Peckhamplex");

        user.setName("Amy");
        cinema.addVisitor(user);

        this.neo4jTemplate.save(cinema);

        assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
        assertEquals(2, this.beforeSaveEventListener.getEvents().size());
        assertEquals(2, this.afterSaveEventListener.getEvents().size());

        assertFalse(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertFalse(this.afterDeleteEventListener.hasReceivedAnEvent());

        initialiseEventListeners();

        this.neo4jTemplate.delete(user);
        // delete events on user
        assertTrue(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertEquals(1, this.beforeDeleteEventListener.getEvents().size());
        assertSame(user, this.beforeDeleteEventListener.getEvents().get(0).getEntity());

        assertTrue(this.afterDeleteEventListener.hasReceivedAnEvent());
        assertEquals(1, this.afterDeleteEventListener.getEvents().size());
        assertSame(user, this.afterDeleteEventListener.getEvents().get(0).getEntity());

        // save/update events on cinema as well
        assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertEquals(1, this.beforeSaveEventListener.getEvents().size());
        assertSame(cinema, this.beforeSaveEventListener.getEvents().get(0).getEntity());

        assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
        assertEquals(1, this.afterSaveEventListener.getEvents().size());
        assertSame(cinema, this.afterSaveEventListener.getEvents().get(0).getEntity());


    }
}
