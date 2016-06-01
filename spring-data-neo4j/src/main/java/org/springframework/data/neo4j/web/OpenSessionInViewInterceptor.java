package org.springframework.data.neo4j.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.SessionFactoryUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Created by markangrish on 13/05/2016.
 */
public class OpenSessionInViewInterceptor implements WebRequestInterceptor, BeanFactoryAware {

	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		setSessionFactory(beanFactory.getBean(SessionFactory.class));
	}

	protected Session openSession() throws IllegalStateException {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory specified");
		return sessionFactory.openSession();
	}

	@Override
	public void preHandle(WebRequest webRequest) throws Exception {
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// Do not modify the Session: just mark the request accordingly.
			String participateAttributeName = getParticipateAttributeName();
			Integer count = (Integer) webRequest.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			webRequest.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		} else {
			logger.debug("Opening Neo4j Session in OpenSessionInViewInterceptor");
			Session session = openSession();
			TransactionSynchronizationManager.bindResource(
					getSessionFactory(), new SessionHolder(session));
		}
	}

	@Override
	public void postHandle(WebRequest webRequest, ModelMap modelMap) throws Exception {

	}

	@Override
	public void afterCompletion(WebRequest webRequest, Exception e) throws Exception {
		if (!decrementParticipateCount(webRequest)) {
			SessionHolder emHolder = (SessionHolder)
					TransactionSynchronizationManager.unbindResource(getSessionFactory());
			logger.debug("Closing Noe4j Session in OpenSessionInViewInterceptor");
			SessionFactoryUtils.closeSession(emHolder.getSession());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count == null) {
			return false;
		}
		// Do not modify the Session: just clear the marker.
		if (count > 1) {
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		} else {
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	private String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}
}
