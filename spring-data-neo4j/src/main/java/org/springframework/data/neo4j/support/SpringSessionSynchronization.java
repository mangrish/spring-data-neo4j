package org.springframework.data.neo4j.support;


import org.neo4j.ogm.session.Session;
import org.springframework.core.Ordered;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.session.SessionFactoryUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Callback for resource cleanup at the end of a Spring-managed transaction
 * for a pre-bound Neo4j OGM Session.
 *
 * @author Mark Angrish
 */
public class SpringSessionSynchronization implements TransactionSynchronization, Ordered {

	private final SessionHolder sessionHolder;

	private final SessionFactory sessionFactory;

	private final boolean newSession;

	private boolean holderActive = true;


	public SpringSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory) {
		this(sessionHolder, sessionFactory, false);
	}

	public SpringSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory, boolean newSession) {
		this.sessionHolder = sessionHolder;
		this.sessionFactory = sessionFactory;
		this.newSession = newSession;
	}


	private Session getCurrentSession() {
		return this.sessionHolder.getSession();
	}


	@Override
	public int getOrder() {
		return SessionFactoryUtils.SESSION_SYNCHRONIZATION_ORDER;
	}

	@Override
	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.sessionFactory);
			// Eagerly disconnect the Session here, to make release mode "on_close" work on JBoss.
		}
	}

	@Override
	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
		}
	}

	@Override
	public void flush() {

	}

	@Override
	public void beforeCommit(boolean readOnly) {

	}


	@Override
	public void beforeCompletion() {

		if (this.newSession) {
			TransactionSynchronizationManager.unbindResource(this.sessionFactory);
			this.holderActive = false;
		}
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
		try {
			if (status != STATUS_COMMITTED) {
				// Clear all pending inserts/updates/deletes in the Session.
				// Necessary for pre-bound Sessions, to avoid inconsistent state.
				this.sessionHolder.getSession().clear();
			}
		} finally {
			this.sessionHolder.setSynchronizedWithTransaction(false);
			// Call close() at this point if it's a new Session...
			if (this.newSession) {
				SessionFactoryUtils.closeSession(sessionFactory, this.sessionHolder.getSession());
			}
		}
	}
}
