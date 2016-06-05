package org.springframework.data.neo4j.session;

import org.neo4j.ogm.session.Session;

/**
 * Defines the contract for implementations which know how to scope the notion
 * of a {@link SessionFactory#getCurrentSession() current session}.
 * <p/>
 * Implementations should adhere to the following:
 * <ul>
 * <li>contain a constructor accepting a single argument of type
 * {@link SessionFactoryImpl}
 * <li>should be thread safe
 * <li>should be fully serializable
 * </ul>
 * <p/>
 * Implementors should be aware that they are also fully responsible for
 * cleanup of any generated current-sessions.
 * <p/>
 * Note that there will be exactly one instance of the configured
 * SessionContext implementation per {@link SessionFactory}.
 *
 * @author Mark Angrish
 */
public interface SessionContext {

	/**
	 * Retrieve the current session according to the scoping defined
	 * by this implementation.
	 *
	 * @return The current session.
	 * @throws RuntimeException Typically indicates an issue
	 * locating or creating the current session.
	 */
	Session currentSession() throws RuntimeException;

	void closeSession(Session session);
}
