package org.springframework.ogm.neo4j.transactions;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.neo4j.ogm.transaction.Transaction;

/**
 * Created by markangrish on 14/05/2016.
 */
public class Neo4jTransactionManagerTests {

	private SessionFactoryProvider factory;

	private Session session;

	private Transaction tx;
}
