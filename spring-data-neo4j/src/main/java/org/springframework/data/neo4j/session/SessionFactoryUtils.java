package org.springframework.data.neo4j.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.exception.NotFoundException;
import org.neo4j.ogm.exception.ResultProcessingException;
import org.neo4j.ogm.exception.TransactionException;
import org.neo4j.ogm.session.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.data.neo4j.support.UncategorizedGraphStoreException;


/**
 * Created by markangrish on 14/05/2016.
 */
public class SessionFactoryUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);
	public static final int SESSION_SYNCHRONIZATION_ORDER = 1000;


	public static void closeSession(SessionFactory sessionFactory, Session session) {
		if (session != null) {
			try {
				sessionFactory.closeSession(session);
			}
			catch (RuntimeException ex) {
				logger.debug("Could not close Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Unexpected exception on closing Hibernate Session", ex);
			}
		}
	}

	public static DataAccessException convertNeo4jOgmAccessException(RuntimeException ex) {
		if (ex instanceof NotFoundException) {
			return new DataRetrievalFailureException(ex.getMessage(), ex);
		}

		if (ex instanceof InvalidDepthException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		if (ex instanceof ResultProcessingException) {
			return new DataRetrievalFailureException(ex.getMessage(), ex);
		}

		if (ex instanceof TransactionException) {
			return new UncategorizedDataAccessException(ex.getMessage(), ex) {
			};
		}

		return new UncategorizedGraphStoreException(ex.getMessage(), ex);
	}
}
