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
package org.springframework.data.neo4j.transactions.service;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ogm.neo4j.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Vince Bickers
 */
@Component
public class BusinessService {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void successMethodInTransaction() {
        insert();
    }

    @Transactional // throws unchecked exception
    public void failMethodInTransaction() {
        insert();
        throw new RuntimeException("Deliberately throwing exception");
    }

    // transactional only from service wrapper, throws checked exception
    public void throwsException() throws Exception {
        insert();
        throw new Exception("Deliberately throwing exception");
    }

    public void insert() {
        new Neo4jTemplate(sessionFactory).execute("CREATE (node {name: 'n'})");
    }

    public Iterable<Map<String, Object>> fetch() {
        return new Neo4jTemplate(sessionFactory).query("MATCH (n) RETURN n.name", new HashMap<String, Object>());
    }

    public void purge() {
        new Neo4jTemplate(sessionFactory).execute("MATCH (n) DELETE n");

    }

}
