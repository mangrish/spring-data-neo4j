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

package org.springframework.data.neo4j.repositories.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.LocalSessionFactoryBean;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;

/**
 * @author Michal Bachman
 */
@Configuration
@ComponentScan({"org.springframework.data.neo4j.repositories"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.repositories.repo")
@EnableTransactionManagement
public class RepositoriesTestContext {

	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		SessionFactory sessionFactory = sessionFactory();
		Assert.notNull(sessionFactory, "You must provide a SessionFactory instance in your Spring configuration classes");
		return new Neo4jTransactionManager(sessionFactory);
	}

	@Bean
	public SessionFactory sessionFactory() {
		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setPackagesToScan("org.springframework.data.neo4j.repositories.domain");
		lsfb.afterPropertiesSet();
		return lsfb.getObject();
	}
}
