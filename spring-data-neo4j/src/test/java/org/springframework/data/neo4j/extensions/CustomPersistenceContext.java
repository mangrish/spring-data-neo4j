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
package org.springframework.data.neo4j.extensions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.LocalSessionFactoryBean;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 *
 * Note how the repository base class for all our repositories is overridden
 * using the 'repositoryBaseClass' attribute.
 * This annotation change allows all our repositories to easily extend one or more
 * additional interfaces.
 *
 * @author: Vince Bickers
 */
@Configuration
@EnableNeo4jRepositories(repositoryBaseClass = CustomGraphRepositoryImpl.class)
@EnableTransactionManagement
public class CustomPersistenceContext {

    @Bean
    public Neo4jOperations neo4jTemplate() throws Exception {
        return new Neo4jTemplate(getSessionFactory());
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        Neo4jTransactionManager transactionManager = new Neo4jTransactionManager();
        transactionManager.setSessionFactory(getSessionFactory());
        transactionManager.afterPropertiesSet();
        return transactionManager;
    }

    @Bean
    public SessionFactory getSessionFactory() {
        LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
        lsfb.setPackagesToScan("org.springframework.data.neo4j.extensions.domain");
        lsfb.afterPropertiesSet();
        return lsfb.getObject();
    }
}
