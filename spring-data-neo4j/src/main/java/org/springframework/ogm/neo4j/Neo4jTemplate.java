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

package org.springframework.ogm.neo4j;


import static org.springframework.data.neo4j.util.IterableUtils.*;

import java.util.Collection;
import java.util.Map;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.event.*;
import org.springframework.util.Assert;

/**
 * Spring Data template for Neo4j, which is an implementation of {@link Neo4jOperations}.  Indeed, framework users are encouraged
 * to favour coding against the {@link Neo4jOperations} interface rather than the {@link Neo4jTemplate} directly, as the
 * interface API will be more consistent over time and enhanced proxy objects of the interface may actually be created by Spring
 * for auto-wiring instead of this template.
 * <p>
 * Note that this class also implements {@link ApplicationEventPublisherAware} and will publish events before data manipulation
 * operations - specifically delete and save.
 * </p>
 * Please note also that all methods on this class throw a {@link DataAccessException} if any underlying {@code Exception} is
 * thrown. Since {@link DataAccessException} is a runtime exception, this is not documented at the method level.
 *
 * @author Adam George
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
public class Neo4jTemplate implements Neo4jOperations, ApplicationEventPublisherAware {

	private final Session session;
	private ApplicationEventPublisher applicationEventPublisher;

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
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public <T> T execute(Neo4jCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		return action.doInNeo4j(session);
	}

	@Override
	public <T> T load(final Class<T> type, final Long id) {
		return execute(new Neo4jCallback<T>() {
			@Override
			public T doInNeo4j(Session session) throws RuntimeException {
				return session.load(type, id);
			}
		});
	}

	@Override
	public <T> T load(final Class<T> type, final Long id, final int depth) {
		return execute(new Neo4jCallback<T>() {
			@Override
			public T doInNeo4j(Session session) throws RuntimeException {
				return session.load(type, id, depth);
			}
		});
	}

	public <T> Collection<T> loadAll(final Class<T> type, final Collection<Long> ids) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, ids);
			}
		});
	}

	public <T> Collection<T> loadAll(final Class<T> type, final Collection<Long> ids, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, ids, depth);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Class<T> type) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Class<T> type, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, depth);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Class<T> type, final SortOrder sortOrder, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, sortOrder, depth);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Class<T> type, final SortOrder sortOrder, final Pagination pagination, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, sortOrder, pagination, depth);
			}
		});
	}

	public <T> Collection<T> loadAll(final Collection<T> objects) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(objects);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Collection<T> objects, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(objects, depth);
			}
		});
	}

	@Override
	public <T> Collection<T> loadAll(final Class<T> type, final Collection<Long> ids, final SortOrder sortOrder, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, ids, sortOrder, depth);
			}
		});
	}

	@Override
	public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue) {
		return loadByProperty(type, propertyName, propertyValue, 1);
	}

	@Override
	public <T> T loadByProperty(final Class<T> type, final String propertyName, final Object propertyValue, final int depth) {
		return getSingle(loadAllByProperty(type, propertyName, propertyValue, depth));
	}

	@Override
	public <T> Collection<T> loadAllByProperty(final Class<T> type, final String name, final Object value) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, new Filter(name, value));
			}
		});
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
	public <T> Collection<T> loadAllByProperties(final Class<T> type, final Filters parameters, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, parameters, depth);
			}
		});
	}

	public <T> Collection<T> loadAllByProperty(final Class<T> type, final String name, final Object value, final int depth) {
		return execute(new Neo4jCallback<Collection<T>>() {
			@Override
			public Collection<T> doInNeo4j(Session session) throws RuntimeException {
				return session.loadAll(type, new Filter(name, value), depth);
			}
		});
	}

	@Override
	public void delete(final Object entity) {
		execute(new Neo4jCallback<Void>() {
			@Override
			public Void doInNeo4j(Session session) throws RuntimeException {
				publishEvent(new BeforeDeleteEvent(this, entity));
				session.delete(entity);
				publishEvent(new AfterDeleteEvent(this, entity));
				return null;
			}
		});
	}

	@Override
	public void clear() {
		execute(new Neo4jCallback<Void>() {
			@Override
			public Void doInNeo4j(Session session) throws RuntimeException {
				session.clear();
				return null;
			}
		});
	}

	public <T> void deleteAll(final Class<T> type) {
		execute(new Neo4jCallback<Object>() {
			@Override
			public Object doInNeo4j(Session session) throws RuntimeException {
				session.deleteAll(type);
				return null;
			}
		});
	}

	@Override
	public QueryStatistics execute(final String jsonStatements) {
		return execute(new Neo4jCallback<QueryStatistics>() {
			@Override
			public QueryStatistics doInNeo4j(Session session) throws RuntimeException {
				return session.query(jsonStatements, Utils.map()).queryStatistics();
			}
		});
	}

	@Override
	public QueryStatistics execute(final String cypher, final Map<String, Object> parameters) {
		return execute(new Neo4jCallback<QueryStatistics>() {
			@Override
			public QueryStatistics doInNeo4j(Session session) throws RuntimeException {
				return session.query(cypher, parameters).queryStatistics();
			}
		});
	}

	@Override
	public <T> T save(final T entity) {
		return execute(new Neo4jCallback<T>() {
			@Override
			public T doInNeo4j(Session session) throws RuntimeException {
				publishEvent(new BeforeSaveEvent(this, entity));
				session.save(entity);
				publishEvent(new AfterSaveEvent(this, entity));
				return entity;
			}
		});
	}

	public <T> T save(final T entity, final int depth) {
		return execute(new Neo4jCallback<T>() {
			@Override
			public T doInNeo4j(Session session) throws RuntimeException {
				publishEvent(new BeforeSaveEvent(this, entity));
				session.save(entity, depth);
				publishEvent(new AfterSaveEvent(this, entity));
				return entity;
			}
		});
	}

	@Override
	public Result query(final String cypher, final Map<String, ?> parameters) {
		return execute(new Neo4jCallback<Result>() {
			@Override
			public Result doInNeo4j(Session session) throws RuntimeException {
				return session.query(cypher, parameters);
			}
		});
	}

	@Override
	public <T> Iterable<T> queryForObjects(final Class<T> objectType, final String cypher, final Map<String, ?> parameters) {
		return execute(new Neo4jCallback<Iterable<T>>() {
			@Override
			public Iterable<T> doInNeo4j(Session session) throws RuntimeException {
				return session.query(objectType, cypher, parameters);
			}
		});
	}

	@Override
	public Result query(final String cypher, final Map<String, ?> parameters, final boolean readOnly) {
		return execute(new Neo4jCallback<Result>() {
			@Override
			public Result doInNeo4j(Session session) throws RuntimeException {
				return session.query(cypher, parameters, readOnly);
			}
		});
	}

	@Override
	public <T> T queryForObject(final Class<T> objectType, final String cypher, final Map<String, ?> parameters) {
		return execute(new Neo4jCallback<T>() {
			@Override
			public T doInNeo4j(Session session) throws RuntimeException {
				return session.queryForObject(objectType, cypher, parameters);
			}
		});
	}

	@Override
	public long count(final Class<?> entityClass) {
		return execute(new Neo4jCallback<Long>() {
			@Override
			public Long doInNeo4j(Session session) throws RuntimeException {
				return session.countEntitiesOfType(entityClass);
			}
		});
	}

	private void publishEvent(Neo4jDataManipulationEvent event) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(event);
		}
	}
}
