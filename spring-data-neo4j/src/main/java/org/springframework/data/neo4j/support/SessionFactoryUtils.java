package org.springframework.data.neo4j.support;

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

/**
 * Created by markangrish on 14/05/2016.
 */
public class SessionFactoryUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);


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

	public static Session getTransactionalSession(SessionFactory targetFactory, boolean synchronizedWithTransaction) {
		return null;
	}

	public static void closeSession(Session target) {

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
