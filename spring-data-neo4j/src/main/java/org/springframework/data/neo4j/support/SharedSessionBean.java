package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.util.Assert;

/**
 * Created by markangrish on 20/05/2016.
 */
public class SharedSessionBean
		implements FactoryBean<Session>, InitializingBean, BeanFactoryAware {

	/**
	 * Logger available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactoryProvider sessionFactoryProvider;

	private boolean synchronizedWithTransaction = true;

	private Session shared;


	public void setSessionFactoryProvider(SessionFactoryProvider sessionFactoryProvider) {
		this.sessionFactoryProvider = sessionFactoryProvider;
	}

	public SessionFactoryProvider getSessionFactoryProvider() {
		return this.sessionFactoryProvider;
	}


	/**
	 * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
	 * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
//		if (getSessionFactoryProvider() == null) {
//			if (!(beanFactory instanceof ListableBeanFactory)) {
//				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
//						"in a non-listable BeanFactory: " + beanFactory);
//			}
//			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
//			setSessionFactoryProvider(SessionFactoryProviderUtils.findSessionFactoryProvider(lbf));
//		}
	}


	protected Session createSession() throws IllegalStateException {
		SessionFactoryProvider sessionFactoryProvider = getSessionFactoryProvider();
		Assert.state(sessionFactoryProvider != null, "No SessionFactoryProvider specified");
		return sessionFactoryProvider.openSession();
	}

	/**
	 * Obtain the transactional EntityManager for this accessor's EntityManagerFactory, if any.
	 *
	 * @return the transactional EntityManager, or {@code null} if none
	 * @throws IllegalStateException if this accessor is not configured with an EntityManagerFactory
	 */
	protected Session getTransactionalSession() throws IllegalStateException {
		SessionFactoryProvider sessionFactoryProvider = getSessionFactoryProvider();
		Assert.state(sessionFactoryProvider != null, "No SessionFactoryProvider specified");
		return SessionFactoryProviderUtils.getSession(sessionFactoryProvider, true);
	}


	/**
	 * Set whether to automatically join ongoing transactions (according
	 * to the JPA 2.1 SynchronizationType rules). Default is "true".
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}


	@Override
	public final void afterPropertiesSet() {
		SessionFactoryProvider sessionFactoryProvider = getSessionFactoryProvider();
		if (sessionFactoryProvider == null) {
			throw new IllegalArgumentException("'sessionFactoryProvider' is required");
		}

		this.shared = SharedSessionCreator.createSharedSession(
				sessionFactoryProvider, this.synchronizedWithTransaction);
	}


	@Override
	public Session getObject() {
		return this.shared;
	}

	@Override
	public Class<? extends Session> getObjectType() {
		return Session.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
