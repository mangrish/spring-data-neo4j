package org.springframework.data.neo4j.support;

import javax.transaction.TransactionRequiredException;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.annotations.EntityAccessException;
import org.neo4j.ogm.exception.*;
import org.neo4j.ogm.session.Session;
import org.springframework.core.Ordered;
import org.springframework.dao.*;
import org.springframework.data.neo4j.session.Neo4jSessionFactory;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Created by markangrish on 14/05/2016.
 */
public class SessionFactoryUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);

	private static Method createSessionWithSynchronizationTypeMethod;

	private static Object synchronizationTypeUnsynchronized;

	static {
		try {
			@SuppressWarnings( "rawtypes" )
			Class<Enum> synchronizationTypeClass = (Class<Enum>) ClassUtils.forName(
					"javax.persistence.SynchronizationType", SessionFactoryUtils.class.getClassLoader());
			createSessionWithSynchronizationTypeMethod = SessionFactory.class.getMethod(
					"openSession", synchronizationTypeClass);
			synchronizationTypeUnsynchronized = Enum.valueOf(synchronizationTypeClass, "UNSYNCHRONIZED");
		}
		catch (Exception ex) {
			// No JPA 2.1 API available
			createSessionWithSynchronizationTypeMethod = null;
		}
	}

	public static Session getSession(SessionFactory sessionFactory, boolean allowCreate) throws IllegalStateException {

		Assert.notNull(sessionFactory, "No SessionFactory specified");

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

		if (sessionHolder != null) {
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				sessionHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new SessionSynchronization(sessionHolder, sessionFactory, false));
			}
			return sessionHolder.getSession();
		}

		if (!allowCreate && !TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("No Neo4j Session bound to thread, " +
					"and configuration does not allow creation of non-transactional one here");
		}

		logger.debug("Opening Neo4j Session");
		Session session = sessionFactory.openSession();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for Neo4j Session");
			// Use same Session for further Neo4j actions within the transaction.
			// Thread object will get removed by synchronization at transaction completion.
			sessionHolder = new SessionHolder(session);
			sessionHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new SessionSynchronization(sessionHolder, sessionFactory, true));
			TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
		}

		return session;
	}

	public static DataAccessException convertNeo4jAccessExceptionIfPossible(RuntimeException ex) {

		if (ex instanceof AmbiguousBaseClassException || ex instanceof BaseClassNotFoundException
				|| ex instanceof InvalidDepthException || ex instanceof MappingException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		if (ex instanceof CypherException || ex instanceof MissingOperatorException) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
		}

		if (ex instanceof NotFoundException) {
			return new DataRetrievalFailureException(ex.getMessage(), ex);
		}

		if (ex instanceof UnknownStatementTypeException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		if (ex instanceof ConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		if (ex instanceof TransactionManagerException || ex instanceof EntityAccessException
				|| ex instanceof TransactionException || ex instanceof ServiceNotFoundException
				|| ex instanceof ResultProcessingException || ex instanceof ResultErrorsException) {
			return new Neo4jSystemException(ex);
		}

		// If we get here, we have an exception that resulted from user code,
		// rather than the persistence provider, so we return null to indicate
		// that translation should not occur.
		return null;
	}

	public static boolean hasTransactionalSession(Neo4jSessionFactory sessionFactory) {
		if (sessionFactory == null) {
			return false;
		}
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		return (sessionHolder != null && (sessionHolder.getSession() != null));
	}

	public static boolean isSessionTransactional(Session session, Neo4jSessionFactory sessionFactory) {
		if (sessionFactory == null) {
			return false;
		}
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		return (sessionHolder != null && (sessionHolder.getSession() == session));
	}

	public static Session getTransactionalSession(SessionFactory sessionFactory, boolean synchronizedWithTransaction) {
		Assert.notNull(sessionFactory, "No SessionFactory specified");

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		if (sessionHolder != null) {
			if (synchronizedWithTransaction) {
				if (!sessionHolder.isSynchronizedWithTransaction()) {
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						// Try to explicitly synchronize the EntityManager itself
						// with an ongoing JTA transaction, if any.
//						try {
//							//sessionHolder.getSession().joinTransaction();
//						}
//						catch (TransactionRequiredException ex) {
//							logger.debug("Could not join transaction because none was actually active", ex);
//						}
					}
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						Object transactionData = prepareTransaction(sessionHolder.getSession(), sessionFactory);
						TransactionSynchronizationManager.registerSynchronization(
								new TransactionalSessionSynchronization(sessionHolder, sessionFactory, transactionData, false));
						sessionHolder.setSynchronizedWithTransaction(true);
					}
				}
				// Use holder's reference count to track synchronizedWithTransaction access.
				// isOpen() check used below to find out about it.
				sessionHolder.requested();
				return sessionHolder.getSession();
			}
			else {
				// unsynchronized EntityManager demanded
				if (!sessionHolder.isOpen()) {
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						return null;
					}
					// EntityManagerHolder with an active transaction coming from JpaTransactionManager,
					// with no synchronized EntityManager having been requested by application code before.
					// Unbind in order to register a new unsynchronized EntityManager instead.
					TransactionSynchronizationManager.unbindResource(sessionFactory);
				}
				else {
					// Either a previously bound unsynchronized EntityManager, or the application
					// has requested a synchronized EntityManager before and therefore upgraded
					// this transaction's EntityManager to synchronized before.
					return sessionHolder.getSession();
				}
			}
		}
		else if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			// Indicate that we can't obtain a transactional EntityManager.
			return null;
		}

		// Create a new EntityManager for use within the current transaction.
		logger.debug("Opening JPA EntityManager");
		Session session = null;
		if (!synchronizedWithTransaction && createSessionWithSynchronizationTypeMethod != null) {
			try {
				session = (Session) ReflectionUtils.invokeMethod(createSessionWithSynchronizationTypeMethod,
						sessionFactory, synchronizationTypeUnsynchronized);
			}
			catch (AbstractMethodError err) {
				// JPA 2.1 API available but method not actually implemented in persistence provider:
				// falling back to regular createEntityManager method.
			}
		}
		if (session == null) {
			session = sessionFactory.openSession();
		}

		// Use same EntityManager for further JPA operations within the transaction.
		// Thread-bound object will get removed by synchronization at transaction completion.
		logger.debug("Registering transaction synchronization for JPA EntityManager");
		sessionHolder = new SessionHolder(session);
		if (synchronizedWithTransaction) {
			Object transactionData = prepareTransaction(session, sessionFactory);
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalSessionSynchronization(sessionHolder, sessionFactory, transactionData, true));
			sessionHolder.setSynchronizedWithTransaction(true);
		}
		else {
			// Unsynchronized - just scope it for the transaction, as demanded by the JPA 2.1 spec...
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionScopedSessionSynchronization(sessionHolder, sessionFactory));
		}
		TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

		return session;
	}

	private static Object prepareTransaction(Session em, SessionFactory emf) {

		return null;
	}

	public static void closeSession(Session target) {

	}



	/**
	 * Callback for resource cleanup at the end of a non-JPA transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class TransactionalSessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactory>
			implements Ordered {

		private final Object transactionData;

		private final boolean newEntityManager;

		public TransactionalSessionSynchronization(
				SessionHolder emHolder, SessionFactory emf, Object txData, boolean newEm) {
			super(emHolder, emf);
			this.transactionData = txData;
			this.newEntityManager = newEm;
		}

		@Override
		public int getOrder() {
			return 1000;
		}


		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newEntityManager;
		}

		@Override
		protected void cleanupResource(SessionHolder resourceHolder, SessionFactory resourceKey, boolean committed) {
			if (!committed) {
				// Clear all pending inserts/updates/deletes in the EntityManager.
				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
				resourceHolder.getSession().clear();
			}
		}
	}

	private static class TransactionScopedSessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactory>
			implements Ordered {

		public TransactionScopedSessionSynchronization(SessionHolder emHolder, SessionFactory emf) {
			super(emHolder, emf);
		}

		@Override
		public int getOrder() {
			return 900;
		}
	}

	private static class SessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactory>
			implements Ordered {

		private final boolean newSession;

		public SessionSynchronization(
				SessionHolder sessionHolder, SessionFactory sessionFactory, boolean newSession) {
			super(sessionHolder, sessionFactory);
			this.newSession = newSession;
		}

		@Override
		public int getOrder() {
			return 900;
		}

		@Override
		public void flushResource(SessionHolder resourceHolder) {
//			resourceHolder.session().clear();
		}

		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newSession;
		}

		@Override
		protected boolean shouldReleaseAfterCompletion(SessionHolder resourceHolder) {
//			return !resourceHolder.session().isClosed();
			return false;
		}
	}
}
