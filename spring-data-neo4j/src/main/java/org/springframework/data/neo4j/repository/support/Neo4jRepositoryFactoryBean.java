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

package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;


/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class Neo4jRepositoryFactoryBean<S extends Repository<T, Long>, T> extends TransactionalRepositoryFactoryBeanSupport<S, T, Long> {

	private Session session;

	public void setSession(Session session) {
		this.session = session;
	}

	@Autowired
	@Override
	protected void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
	}

	private RepositoryFactorySupport createRepositoryFactory(Session session) {
		return new Neo4jRepositoryFactory(session);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(session, "Session must not be null!");
		super.afterPropertiesSet();
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return createRepositoryFactory(session);
	}
}
