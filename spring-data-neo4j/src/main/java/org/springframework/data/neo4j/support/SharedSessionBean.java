package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.data.neo4j.session.SessionFactory;
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

	private SessionFactory sessionFactory;

	private boolean synchronizedWithTransaction = true;

	private Session shared;


	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}


	/**
	 * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
	 * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
//		if (getSessionFactory() == null) {
//			if (!(beanFactory instanceof ListableBeanFactory)) {
//				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
//						"in a non-listable BeanFactory: " + beanFactory);
//			}
//			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
//			setSessionFactory(SessionFactoryUtils.findSessionFactory(lbf));
//		}
	}


	protected Session createSession() throws IllegalStateException {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory specified");
		return sessionFactory.openSession();
	}

	/**
	 * Obtain the transactional EntityManager for this accessor's SessionFactory, if any.
	 *
	 * @return the transactional EntityManager, or {@code null} if none
	 * @throws IllegalStateException if this accessor is not configured with an SessionFactory
	 */
	protected Session getTransactionalSession() throws IllegalStateException {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory specified");
		return SessionFactoryUtils.getSession(sessionFactory, true);
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
		SessionFactory sessionFactory = getSessionFactory();
		if (sessionFactory == null) {
			throw new IllegalArgumentException("'sessionFactory' is required");
		}

		this.shared = SharedSessionCreator.createSharedSession(
				sessionFactory, this.synchronizedWithTransaction);
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
