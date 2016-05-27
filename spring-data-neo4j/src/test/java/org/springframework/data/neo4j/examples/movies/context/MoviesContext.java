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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.session.Neo4jSessionFactory;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michal Bachman
 */
@Configuration
@ComponentScan({"org.springframework.data.neo4j.examples.movies"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.movies.repo")
@EnableTransactionManagement
public class MoviesContext extends Neo4jConfiguration {

    @Override
    @Bean
    public SessionFactory sessionFactory() {
        return new Neo4jSessionFactory("org.springframework.data.neo4j.examples.movies.domain");
    }
}
