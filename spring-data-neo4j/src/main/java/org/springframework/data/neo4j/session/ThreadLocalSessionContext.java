package org.springframework.data.neo4j.session;

import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 02/06/2016.
 */
public class ThreadLocalSessionContext implements SessionContext {

	private final ThreadLocal<Session> sessions = new ThreadLocal<>();
	private final SessionFactoryImpl sessionFactory;

	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 *
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public ThreadLocalSessionContext(SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Session currentSession() throws RuntimeException {
		Session currentSession = sessions.get();

		if (currentSession == null) {
			currentSession = sessionFactory.openSession();
			sessions.set(currentSession);
		}

		return currentSession;
	}

	@Override
	public void closeSession(Session session) {
		Session currentSession = sessions.get();

		if (currentSession != null) {
			sessions.remove();
		}
	}
}
