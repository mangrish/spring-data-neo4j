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

package org.springframework.data.neo4j.examples.movies.context;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.ogm.neo4j.LocalSessionFactoryProviderBean;
import org.springframework.ogm.neo4j.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michal Bachman
 */
@Configuration
@ComponentScan({"org.springframework.data.neo4j.examples.movies"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.movies.repo")
@EnableTransactionManagement
public class MoviesContext {

	@Bean
	public PlatformTransactionManager transactionManager(SessionFactoryProvider sessionFactoryProvider) throws Exception {
		return new Neo4jTransactionManager(sessionFactoryProvider);
	}

	@Bean
	public SessionFactoryProvider sessionFactoryProvider() {
		LocalSessionFactoryProviderBean lsfb = new LocalSessionFactoryProviderBean();
		lsfb.setPackagesToScan("org.springframework.data.neo4j.examples.movies.domain");
		lsfb.afterPropertiesSet();
		return lsfb.getObject();
	}
}
