package org.springframework.ogm.neo4j;

import org.neo4j.ogm.session.Session;


public interface Neo4jCallback<T> {

	T doInNeo4j(Session session) throws RuntimeException;
}
