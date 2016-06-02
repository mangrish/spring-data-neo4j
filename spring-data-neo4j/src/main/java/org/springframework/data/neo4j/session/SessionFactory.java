package org.springframework.data.neo4j.session;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 02/06/2016.
 */
public interface SessionFactory {

	/**
	 * Retrieves the meta-data that was built up when this {@link org.neo4j.ogm.session.SessionFactory} was constructed.
	 *
	 * @return The underlying {@link MetaData}
	 */
	MetaData getMetaData();

	/**
	 * Opens a new Neo4j mapping {@link Session} using the Driver specified in the OGM configuration
	 * The driver should be configured to connect to the database using the appropriate
	 * DriverConfig
	 *
	 * @return A new {@link Session}
	 */
	Session openSession();


	/**
	 * Obtains the current session.
	 *
	 * @return The current session.
	 */
	Session getCurrentSession();

	/**
	 * Closes the current session.
	 *
	 * TODO: This method should be moved to Session and Session should extend AutoClosable.
	 *
	 * @param session The session to close;
	 */
	void closeSession(Session session);

}
