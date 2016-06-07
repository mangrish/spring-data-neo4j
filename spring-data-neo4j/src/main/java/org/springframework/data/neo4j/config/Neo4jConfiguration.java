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


import javax.annotation.Resource;

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
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.Neo4jOgmExceptionTranslator;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The base Spring configuration bean from which users are recommended to inherit when setting up Spring Data Neo4j.
 *
 * @author Vince Bickers
 */
@Configuration
public abstract class Neo4jConfiguration {

	private final Logger logger = LoggerFactory.getLogger(Neo4jConfiguration.class);

	@Bean
	public Neo4jOperations neo4jTemplate() throws Exception {
		return new Neo4jTemplate(getSessionFactory());
	}

	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		logger.info("Initialising Neo4jTransactionManager");
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager();
		transactionManager.setSessionFactory(getSessionFactory());
		transactionManager.afterPropertiesSet();
		return transactionManager;
	}

	@Bean
	public abstract SessionFactory getSessionFactory() throws Exception;
}
