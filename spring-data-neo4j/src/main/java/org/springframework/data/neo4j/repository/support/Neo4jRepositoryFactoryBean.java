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

import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.ogm.neo4j.Neo4jOperations;
import org.springframework.ogm.neo4j.SessionFactoryUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;


/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class Neo4jRepositoryFactoryBean<S extends Repository<T, Long>, T> extends TransactionalRepositoryFactoryBeanSupport<S, T, Long> {

    @Autowired
    private SessionFactoryProvider sessionFactoryProvider;

    @Autowired
    private Neo4jOperations neo4jOperations;

    @Override
    public void afterPropertiesSet() {
        setMappingContext(new Neo4jMappingContext(sessionFactoryProvider.metaData()));
        super.afterPropertiesSet();
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new Neo4jRepositoryFactory(SessionFactoryUtils.getSession(sessionFactoryProvider, true), neo4jOperations);
    }
}
