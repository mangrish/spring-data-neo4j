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

package org.springframework.data.neo4j.template;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * Callback interface for Neo4J code. To be used with {@link Neo4jTemplate}'s
 * execution methods, often as anonymous classes within a method implementation.
 * A typical implementation will call {@code Session.load/find/save} to perform
 * some operations on persistent objects.
 *
 * @author Mark Angrish
 * @see Neo4jTemplate
 * @see Neo4jTransactionManager
 */
public interface Neo4jCallback<T> {

	/**
	 * Gets called by {@code Neo4jTemplate.execute} with an active
	 * Neo4J OGM {@code Session}. Does not need to care about activating
	 * or closing the {@code Session}, or handling transactions.
	 * <p>Allows for returning a result object created within the callback,
	 * i.e. a domain object or a collection of domain objects.
	 * A thrown custom RuntimeException is treated as an application exception:
	 * It gets propagated to the caller of the template.
	 * @param session active Neo4J OGM sessionFactory
	 * @return a result object, or {@code null} if none
	 * @throws RuntimeException if thrown by the Neo4j OGM API
	 * @see Neo4jTemplate#execute
	 */
	T doInNeo4j(Session session) throws RuntimeException;
}
