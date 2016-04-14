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

package org.springframework.data.neo4j.event;

import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Adapter class wrapping Spring's ApplicationEventPublisher to provide interoperability with Neo4j OGM Events.
 *
 * @author Vince Bickers
 */
public class Neo4jDataEventListener implements EventListener {

    private final ApplicationEventPublisher eventPublisher;

    public Neo4jDataEventListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onPreSave(Event event) {
        eventPublisher.publishEvent(new BeforeSaveEvent(event.getObject()));
    }

    @Override
    public void onPostSave(Event event) {
        eventPublisher.publishEvent(new AfterSaveEvent(event.getObject()));
    }

    @Override
    public void onPreDelete(Event event) {
        eventPublisher.publishEvent(new BeforeDeleteEvent(event.getObject()));
    }

    @Override
    public void onPostDelete(Event event) {
        eventPublisher.publishEvent(new AfterDeleteEvent(event.getObject()));
    }

}
