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

package org.springframework.data.neo4j.transaction;

import java.util.EnumSet;

import org.neo4j.ogm.exception.TransactionException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.session.SessionFactoryUtils;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This class is a wrapper around the OGM TransactionManager.
 *
 * @author Vince Bickers
 * @author Mark Angrish
 */
public class Neo4jTransactionManager extends AbstractPlatformTransactionManager implements ResourceTransactionManager, InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);

	private SessionFactory sessionFactory;

	private boolean neo4jManagedSession = false;


	public Neo4jTransactionManager() {
	}

	public Neo4jTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		afterPropertiesSet();
	}


	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the EntityManagerFactory that this instance should manage transactions for.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}


	/**
	 * Set whether to operate on a Neo4j OGM-managed Session instead of a
	 * Spring-managed Session, that is, whether to obtain the Session through
	 * Hibernate's {@link SessionFactory#getCurrentSession()}
	 * instead of {@link SessionFactory#openSession()} (with a Spring
	 * {@link TransactionSynchronizationManager}
	 * check preceding it).
	 * <p>Default is "false", i.e. using a Spring-managed Session: taking the current
	 * thread-bound Session if available (e.g. in an Open-Session-in-View scenario),
	 * creating a new Session for the current transaction otherwise.
	 * <p>Switch this flag to "true" in order to enforce use of a Neo4j OGM-managed Session.
	 * Note that this requires {@link SessionFactory#getCurrentSession()}
	 * to always return a proper Session when called for a Spring-managed transaction;
	 * transaction begin will fail if the {@code getCurrentSession()} call fails.
	 * <p>This mode will typically be used in combination with a custom Hibernate
	 * {@link org.hibernate.context.spi.CurrentSessionContext} implementation that stores
	 * Sessions in a place other than Spring's TransactionSynchronizationManager.
	 * It may also be used in combination with Spring's Open-Session-in-View support
	 * (using Spring's default {@link SpringSessionContext}), in which case it subtly
	 * differs from the Spring-managed Session mode: The pre-bound Session will <i>not</i>
	 * receive a {@code clear()} call (on rollback) or a {@code disconnect()}
	 * call (on transaction completion) in such a scenario; this is rather left up
	 * to a custom SessionContext implementation (if desired).
	 */
	public void setNeo4jManagedSession(boolean neo4jManagedSession) {
		this.neo4jManagedSession = neo4jManagedSession;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;
		return (txObject.hasSpringManagedTransaction() ||
				(this.neo4jManagedSession && txObject.hasNeo4jManagedTransaction()));
	}

	@Override
	protected Object doGetTransaction() {
		Neo4jTransactionObject txObject = new Neo4jTransactionObject();

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() + "] for Neo4j OGM transaction");
			}
			txObject.setSessionHolder(sessionHolder);
		} else if (this.neo4jManagedSession) {
			try {
				Session session = this.sessionFactory.getCurrentSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Found Neo4j-managed Session [" + session + "] for Spring-managed transaction");
				}
				txObject.setExistingSession(session);
			} catch (RuntimeException ex) {
				throw new DataAccessResourceFailureException(
						"Could not obtain Neo4j-managed Session for Spring-managed transaction", ex);
			}
		}

		return txObject;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		Session session = null;

		try {
			if (txObject.getSessionHolder() == null || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Session newSession = getSessionFactory().openSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + newSession + "] for Hibernate transaction");
				}
				txObject.setSession(newSession);
			}

			session = txObject.getSessionHolder().getSession();

			// Not allowed to change the transaction settings of the JDBC Connection.
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				// We should set a specific isolation level but are not allowed to...
				throw new InvalidIsolationLevelException(
						"HibernateTransactionManager is not allowed to support custom isolation levels: " +
								"make sure that its 'prepareConnection' flag is on (the default) and that the " +
								"Hibernate connection release mode is set to 'on_close' (the default for JDBC).");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Not preparing JDBC Connection of Hibernate Session [" + session + "]");
			}

			Transaction hibTx = session.beginTransaction();

			// Add the Hibernate transaction to the session holder.
			txObject.getSessionHolder().setTransaction(hibTx);

			// Bind the session holder to the thread.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(getSessionFactory(), txObject.getSessionHolder());
			}
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);
		} catch (Throwable ex) {
			if (txObject.isNewSession()) {
				try {
					if (isActive(session.getTransaction().status())) {
						session.getTransaction().rollback();
					}
				} catch (Throwable ex2) {
					logger.debug("Could not rollback Session after failed transaction begin", ex);
				} finally {
					SessionFactoryUtils.closeSession(sessionFactory, session);
					txObject.setSessionHolder(null);
				}
			}
			throw new CannotCreateTransactionException("Could not open Hibernate Session for transaction", ex);
		}
	}

	private static boolean isActive(Transaction.Status status) {

		return EnumSet.of(Transaction.Status.OPEN, Transaction.Status.ROLLBACK_PENDING, Transaction.Status.COMMIT_PENDING).contains(status);
	}


	@Override
	protected Object doSuspend(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;
		txObject.setSessionHolder(null);
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());

		return new SuspendedResourcesHolder(sessionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// From non-transactional code running in active transaction synchronization
			// -> can be safely removed, will be closed on transaction completion.
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
		TransactionSynchronizationManager.bindResource(getSessionFactory(), resourcesHolder.getSessionHolder());
	}


	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		try {
			txObject.getSessionHolder().getTransaction().commit();
		} catch (TransactionException ex) {
			// assumably from commit call to the underlying JDBC connection
			throw new TransactionSystemException("Could not commit Hibernate transaction", ex);
		} catch (RuntimeException ex) {
			// assumably failed to flush changes to database
			throw convertNeo4jOgmAccessException(ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		try {
			txObject.getSessionHolder().getTransaction().rollback();
		} catch (TransactionException ex) {
			throw new TransactionSystemException("Could not roll back Hibernate transaction", ex);
		} catch (RuntimeException ex) {
			// Shouldn't really happen, as a rollback doesn't cause a flush.
			throw convertNeo4jOgmAccessException(ex);
		} finally {
			if (!txObject.isNewSession() && !this.neo4jManagedSession) {
				// Clear all pending inserts/updates/deletes in the Session.
				// Necessary for pre-bound Sessions, to avoid inconsistent state.
				txObject.getSessionHolder().getSession().clear();
			}
		}
	}


	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		// Remove the session holder from the thread.
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}

		Session session = txObject.getSessionHolder().getSession();

		if (txObject.isNewSession()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Hibernate Session [" + session + "] after transaction");
			}
			SessionFactoryUtils.closeSession(sessionFactory, session);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not closing pre-bound Hibernate Session [" + session + "] after transaction");
			}
		}
		txObject.getSessionHolder().clear();
	}


	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}

	protected DataAccessException convertNeo4jOgmAccessException(RuntimeException ex) {
		return SessionFactoryUtils.convertNeo4jOgmAccessException(ex);
	}

	@Override
	public Object getResourceFactory() {
		return getSessionFactory();
	}


	private static class Neo4jTransactionObject {

		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private boolean newSession;

//		private Transaction rawTransaction;

		public void setSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = true;
		}

		public void setExistingSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = false;
		}

		public void setSessionHolder(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = false;
			this.newSession = false;
		}

		public SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		public boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		public boolean isNewSession() {
			return this.newSession;
		}

		public boolean hasSpringManagedTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.getTransaction() != null);
		}

		public boolean hasNeo4jManagedTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.getSession().getTransaction() != null &&
					isActive(this.sessionHolder.getSession().getTransaction().status()));
		}

		public void setRollbackOnly() {
			this.sessionHolder.setRollbackOnly();
		}
	}

	private static class SuspendedResourcesHolder {

		private final SessionHolder sessionHolder;


		private SuspendedResourcesHolder(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
		}

		private SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}
	}
}
