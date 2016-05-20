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

package org.springframework.data.neo4j.config;


import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.ogm.neo4j.Neo4jOgmExceptionTranslator;
import org.springframework.ogm.neo4j.Neo4jOperations;
import org.springframework.ogm.neo4j.Neo4jTemplate;
import org.springframework.ogm.neo4j.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import javax.annotation.Resource;

/**
 * The base Spring configuration bean from which users are recommended to inherit when setting up Spring Data Neo4j.
 *
 * @author Vince Bickers
 */
@Configuration
public abstract class Neo4jConfiguration {

    private final Logger logger = LoggerFactory.getLogger(Neo4jConfiguration.class);

    @Resource
    private Environment environment;

    @Bean
    public Session getSession() throws Exception {
        logger.info("Initialising Neo4jSession");
        SessionFactory sessionFactory = getSessionFactory();
        Assert.notNull(sessionFactory, "You must provide a SessionFactory instance in your Spring configuration classes");
        return sessionFactory.openSession();
    }

    @Bean
    public Neo4jOperations neo4jTemplate() throws Exception {
        return new Neo4jTemplate(getSession());
    }

    @Bean
    public Neo4jMappingContext neo4jMappingContext() throws Exception {
        return new Neo4jMappingContext(getSessionFactory().metaData());
    }

    @Bean
    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
        logger.info("Initialising PersistenceExceptionTranslator");
        return new PersistenceExceptionTranslator() {
            @Override
            public DataAccessException translateExceptionIfPossible(RuntimeException e) {
                logger.info("Intercepted exception");
                throw Neo4jOgmExceptionTranslator.translateExceptionIfPossible(e);
            }
        };
    }

    @Bean
    public PersistenceExceptionTranslationInterceptor translationInterceptor() {
        logger.info("Initialising PersistenceExceptionTranslationInterceptor");
        return new PersistenceExceptionTranslationInterceptor(persistenceExceptionTranslator());
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        logger.info("Initialising Neo4jTransactionManager");
        SessionFactory sessionFactory = getSessionFactory();
        Assert.notNull(sessionFactory, "You must provide a SessionFactory instance in your Spring configuration classes");
        return new Neo4jTransactionManager(sessionFactory);
    }

    @Bean
    PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        logger.info("Initialising PersistenceExceptionTranslationPostProcessor");
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean
    public abstract SessionFactory getSessionFactory();

}
