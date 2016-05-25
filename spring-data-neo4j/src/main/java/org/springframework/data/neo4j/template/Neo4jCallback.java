package org.springframework.data.neo4j.template;

import org.neo4j.ogm.session.Session;


public interface Neo4jCallback<T> {

	T doInNeo4j(Session session) throws RuntimeException;
}
