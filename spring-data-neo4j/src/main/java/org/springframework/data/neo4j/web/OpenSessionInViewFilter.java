package org.springframework.data.neo4j.web;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.data.neo4j.support.SessionFactoryProviderUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Created by markangrish on 14/05/2016.
 */
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactoryProvider";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;


	public void setSessionFactoryProviderBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}


	protected String getSessionFactoryProviderBeanName() {
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

		SessionFactoryProvider sessionFactoryProvider = lookupSessionFactoryProvider(request);
		boolean participate = false;

		if (TransactionSynchronizationManager.hasResource(sessionFactoryProvider)) {
			// Do not modify the Session: just set the participate flag.
			participate = true;
		} else {
			logger.debug("Opening Neo4j Session in OpenSessionInViewFilter");
			Session session = SessionFactoryProviderUtils.getSession(sessionFactoryProvider, true);
			TransactionSynchronizationManager.bindResource(sessionFactoryProvider, new SessionHolder(session));
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			if (!participate) {

				TransactionSynchronizationManager.unbindResource(sessionFactoryProvider);
				logger.debug("Closing Neo4j Session in OpenSessionInViewFilter");
			}
		}
	}


	protected SessionFactoryProvider lookupSessionFactoryProvider(HttpServletRequest request) {
		return lookupSessionFactoryProvider();
	}


	protected SessionFactoryProvider lookupSessionFactoryProvider() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactoryProvider '" + getSessionFactoryProviderBeanName() +
					"' for OpenSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryProviderBeanName(), SessionFactoryProvider.class);
	}
}
