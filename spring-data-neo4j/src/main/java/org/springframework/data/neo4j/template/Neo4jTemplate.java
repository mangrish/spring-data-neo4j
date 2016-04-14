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


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.event.Neo4jDataEventListener;
import org.springframework.util.Assert;

import static org.springframework.data.neo4j.util.IterableUtils.getSingle;
import static org.springframework.data.neo4j.util.IterableUtils.getSingleOrNull;

/**
 * Spring Data template for Neo4j, which is an implementation of {@link Neo4jOperations}.  Indeed, framework users are encouraged
 * to favour coding against the {@link Neo4jOperations} interface rather than the {@link Neo4jTemplate} directly, as the
 * interface API will be more consistent over time and enhanced proxy objects of the interface may actually be created by Spring
 * for auto-wiring instead of this template.
 *
 * Please note also that all methods on this class throw a {@link DataAccessException} if any underlying {@code Exception} is
 * thrown. Since {@link DataAccessException} is a runtime exception, this is not documented at the method level.
 *
 * @author Adam George
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Vince Bickers
 */
public class Neo4jTemplate implements Neo4jOperations {

    private final Session session;
    private Map<ApplicationEventPublisher, EventListener> publisherEventListenerMap = new HashMap<>();
    /**
     * Constructs a new {@link Neo4jTemplate} based on the given Neo4j OGM {@link Session}.
     *
     * @param session The Neo4j OGM session upon which to base the template
     */
    @Autowired
    public Neo4jTemplate(Session session) {
        Assert.notNull(session, "Cannot create a Neo4jTemplate without a Session!");
        this.session = session;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        EventListener eventListener = new Neo4jDataEventListener(applicationEventPublisher);
        publisherEventListenerMap.put(applicationEventPublisher, eventListener);
        session.register(eventListener);
    }

    @Override
    public boolean removeApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        return session.dispose(publisherEventListenerMap.get(eventPublisher));
    }

    @Override
    public <T> T load(Class<T> type, Long id) {
        return session.load(type, id);
    }

    @Override
    public <T> T load(Class<T> type, Long id, int depth) {
        return session.load(type, id, depth);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
        return session.loadAll(type, ids);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
        return session.loadAll(type, ids, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type) {
        return session.loadAll(type);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, int depth) {
        return session.loadAll(type, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, int depth) {
        return session.loadAll(type, sortOrder, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination, int depth) {
        return session.loadAll(type, sortOrder, pagination, depth);
    }

    public <T> Collection<T> loadAll(Collection<T> objects) {
        return session.loadAll(objects);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, int depth) {
        return session.loadAll(objects, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, int depth) {
        return session.loadAll(type, ids, sortOrder, depth);
    }

    @Override
    public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue) {
        return loadByProperty(type, propertyName, propertyValue, 1);
    }

    @Override
    public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue, int depth) {
        return getSingle(loadAllByProperty(type, propertyName, propertyValue, depth));
    }

    public <T> T loadByPropertyOrNull(Class<T> type, String propertyName, Object propertyValue) {
        return getSingleOrNull(loadAllByProperty(type, propertyName, propertyValue));
    }

    @Override
    public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value) {
        return session.loadAll(type, new Filter(name, value));
    }

    @Override
    public <T> T loadByProperties(Class<T> type, Filters parameters) {
        return loadByProperties(type, parameters, 1);
    }

    @Override
    public <T> T loadByProperties(Class<T> type, Filters parameters, int depth) {
        return getSingle(loadAllByProperties(type, parameters, depth));
    }

    @Override
    public <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters) {
       return loadAllByProperties(type, parameters, 1);
    }

    @Override
    public <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters, int depth) {
        return session.loadAll(type, parameters, depth);
    }

    public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value, int depth) {
        return session.loadAll(type, new Filter(name, value), depth);
    }

    @Override
    public void delete(Object entity) {
        session.delete(entity);
    }

    @Override
    public void clear() {
        session.clear();
    }

    public <T> void deleteAll(Class<T> type) {
        session.deleteAll(type);
    }

    public void purgeSession() {
        session.clear();
    }

    @Override
    public <T> T save(T entity) {
        session.save(entity);
        return entity;
    }

    public <T> T save(T entity, int depth) {
        session.save(entity, depth);
        return entity;
    }

    @Override
    public Result query(String cypher, Map<String, ?> parameters) {
        return session.query(cypher, parameters);
    }

    @Override
    public <T> Iterable<T> queryForObjects(Class<T> objectType, String cypher, Map<String, ?> parameters) {
        return session.query(objectType, cypher, parameters);
    }

    @Override
    public Result query(String cypher, Map<String, ?> parameters, boolean readOnly) {
        return session.query(cypher, parameters, readOnly);
    }

    @Override
    public <T> T queryForObject(Class<T> objectType, String cypher, Map<String, ?> parameters) {
        return session.queryForObject(objectType, cypher, parameters);
    }

    @Override
    public long count(Class<?> entityClass) {
        return session.countEntitiesOfType(entityClass);
    }

}
