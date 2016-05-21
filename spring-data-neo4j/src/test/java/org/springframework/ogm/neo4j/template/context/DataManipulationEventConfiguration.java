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

package org.springframework.ogm.neo4j.template.context;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.ogm.neo4j.LocalSessionFactoryProviderBean;
import org.springframework.ogm.neo4j.Neo4jTemplate;
import org.springframework.ogm.neo4j.Neo4jTransactionManager;
import org.springframework.ogm.neo4j.template.TestNeo4jEventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Configuration bean for testing data manipulation events supported by <code>Neo4jTemplate</code>.
 *
 * @author Adam George
 * @author Luanne Misquitta
 */
@Configuration
@EnableTransactionManagement
@EnableNeo4jRepositories
public class DataManipulationEventConfiguration {

	@Bean
	public Neo4jTemplate neo4jTemplate() {
		return new Neo4jTemplate(sessionFactoryProvider());
	}

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

	@Bean
	public TestNeo4jEventListener<BeforeSaveEvent> beforeSaveEventListener() {
		return new TestNeo4jEventListener<BeforeSaveEvent>() {
		};
	}

	@Bean
	public TestNeo4jEventListener<AfterSaveEvent> afterSaveEventListener() {
		return new TestNeo4jEventListener<AfterSaveEvent>() {
		};
	}

	@Bean
	public TestNeo4jEventListener<BeforeDeleteEvent> beforeDeleteEventListener() {
		return new TestNeo4jEventListener<BeforeDeleteEvent>() {
		};
	}

	@Bean
	public TestNeo4jEventListener<AfterDeleteEvent> afterDeleteEventListener() {
		return new TestNeo4jEventListener<AfterDeleteEvent>() {
		};
	}
}
