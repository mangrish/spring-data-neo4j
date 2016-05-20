package org.springframework.ogm.neo4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.annotations.EntityAccessException;
import org.neo4j.ogm.exception.*;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.core.Ordered;
import org.springframework.dao.*;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Created by markangrish on 14/05/2016.
 */
public class SessionFactoryProviderUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryProviderUtils.class);


	public static void releaseSession(Session session, SessionFactoryProvider sessionFactoryProvider) {
		if (session == null) {
			return;
		}
	}

	public static Session getSession(SessionFactoryProvider sessionFactoryProvider, boolean allowCreate) throws IllegalStateException {

		Assert.notNull(sessionFactoryProvider, "No SessionFactoryProvider specified");

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactoryProvider);
		if (sessionHolder != null) {
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				sessionHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new SessionSynchronization(sessionHolder, sessionFactoryProvider, false));
			}
			return sessionHolder.getSession();
		}

		if (!allowCreate && !TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("No Neo4j Session bound to thread, " +
					"and configuration does not allow creation of non-transactional one here");
		}

		logger.debug("Opening Neo4j Session");
		Session session = sessionFactoryProvider.openSession();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for Neo4j Session");
			// Use same PersistenceManager for further Neo4j actions within the transaction.
			// Thread object will get removed by synchronization at transaction completion.
			sessionHolder = new SessionHolder(session);
			sessionHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new SessionSynchronization(sessionHolder, sessionFactoryProvider, true));
			TransactionSynchronizationManager.bindResource(sessionFactoryProvider, sessionHolder);
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


	private static class SessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactoryProvider>
			implements Ordered {

		private final boolean newSession;

		public SessionSynchronization(
				SessionHolder sessionHolder, SessionFactoryProvider sessionFactoryProvider, boolean newSession) {
			super(sessionHolder, sessionFactoryProvider);
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

		@Override
		protected void releaseResource(SessionHolder resourceHolder, SessionFactoryProvider resourceKey) {
			releaseSession(resourceHolder.getSession(), resourceKey);
		}
	}
}
