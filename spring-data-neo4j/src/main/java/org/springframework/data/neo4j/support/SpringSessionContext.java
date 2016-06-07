package org.springframework.data.neo4j.support;


import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.session.SessionContext;
import org.springframework.data.neo4j.session.SessionFactoryImpl;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of Hibernate 3.1's SessionContext interface
 * that delegates to Spring's SessionFactoryUtils for providing a
 * Spring-managed current Session.
 * <p>This SessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
@SuppressWarnings("serial")
public class SpringSessionContext implements SessionContext {

	private final SessionFactoryImpl sessionFactory;


	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 *
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public SpringSessionContext(SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;
	}


	/**
	 * Retrieve the Spring-managed Session for the current thread, if any.
	 */
	@Override
	public Session currentSession() {
		Object value = TransactionSynchronizationManager.getResource(this.sessionFactory);
		if (value instanceof Session) {
			return (Session) value;
		} else if (value instanceof SessionHolder) {
			SessionHolder sessionHolder = (SessionHolder) value;
			Session session = sessionHolder.getSession();
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(
						new SpringSessionSynchronization(sessionHolder, this.sessionFactory, false));
				sessionHolder.setSynchronizedWithTransaction(true);
			}
			return session;
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			Session session = this.sessionFactory.openSession();
			SessionHolder sessionHolder = new SessionHolder(session);
			TransactionSynchronizationManager.registerSynchronization(
					new SpringSessionSynchronization(sessionHolder, this.sessionFactory, true));
			TransactionSynchronizationManager.bindResource(this.sessionFactory, sessionHolder);
			sessionHolder.setSynchronizedWithTransaction(true);
			return session;
		} else {
			throw new RuntimeException("Could not obtain transaction-synchronized Session for current thread");
		}
	}

	@Override
	public void closeSession(Session session) {
	}
}
