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

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.events.TestNeo4jEventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author vince
 */
@Configuration
@ComponentScan({"org.springframework.data.neo4j.repositories"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.repositories.repo")
@EnableTransactionManagement
public class EventHandlingContext extends Neo4jConfiguration {

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.repositories.domain");
    }

    @Bean
    public ApplicationListener<BeforeSaveEvent> beforeSaveEventListener() {
        return new TestNeo4jEventListener<BeforeSaveEvent>() {};
    }

    @Bean
    public ApplicationListener<AfterSaveEvent> afterSaveEventListener() {
        return new TestNeo4jEventListener<AfterSaveEvent>() {};
    }

    @Bean
    public ApplicationListener<BeforeDeleteEvent> beforeDeleteEventListener() {
        return new TestNeo4jEventListener<BeforeDeleteEvent>() {};
    }

    @Bean
    public ApplicationListener<AfterDeleteEvent> afterDeleteEventListener() {
        return new TestNeo4jEventListener<AfterDeleteEvent>() {};
    }
}
