package org.springframework.data.neo4j.web;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.neo4j.ogm.session.Session;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.session.SessionFactoryUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;


public class OpenSessionInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;


	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}


	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}


	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}


	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		SessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// Do not modify the Session: just set the participate flag.
			participate = true;
		} else {
			if (!isAsyncDispatch(request)) {
				logger.debug("Opening Neo4j OGM Session in OpenSessionInViewFilter");
				Session session = openSession(sessionFactory);
				SessionHolder sessionHolder = new SessionHolder(session);
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
			}
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			if (!participate) {
				SessionHolder sessionHolder =
						(SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
				if (!isAsyncStarted(request)) {
					logger.debug("Closing Neo4j OGM Session in OpenSessionInViewFilter");
					SessionFactoryUtils.closeSession(sessionFactory, sessionHolder.getSession());
				}
			}
		}
	}


	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}


	protected SessionFactory lookupSessionFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() +
					"' for OpenSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	/**
	 * Open a Session for the SessionFactory that this filter uses.
	 * <p>The default implementation delegates to the {@link SessionFactory#openSession}
	 * method and sets the {@link Session}'s flush mode to "MANUAL".
	 *
	 * @param sessionFactory the SessionFactory that this filter uses
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 */
	protected Session openSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		try {
			return sessionFactory.openSession();
		} catch (RuntimeException ex) {
			throw new DataAccessResourceFailureException("Could not open Neo4j OGM Session", ex);
		}
	}
}
