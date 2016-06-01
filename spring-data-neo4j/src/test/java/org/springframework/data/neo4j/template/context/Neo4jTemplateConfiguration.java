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

package org.springframework.data.neo4j.template.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.LocalSessionFactoryBean;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;

/**
 * @author vince
 */
@Configuration
@EnableTransactionManagement
public class Neo4jTemplateConfiguration {

	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		SessionFactory sessionFactory = sessionFactory();
		Assert.notNull(sessionFactory, "You must provide a SessionFactory instance in your Spring configuration classes");
		return new Neo4jTransactionManager(sessionFactory);
	}

	@Bean
	public Neo4jOperations neo4jTemplate() throws Exception {
		return new Neo4jTemplate(sessionFactory());
	}

	@Bean
	public SessionFactory sessionFactory() {
		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setPackagesToScan("org.springframework.data.neo4j.examples.movies.domain");
		lsfb.afterPropertiesSet();
		return lsfb.getObject();
	}
}
