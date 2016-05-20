package org.springframework.ogm.neo4j.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ogm.neo4j.SessionFactoryProviderUtils;

/**
 * Created by markangrish on 14/05/2016.
 */
public class SpringSessionProxyBean implements FactoryBean<Session>, InitializingBean, BeanFactoryAware {

	private SessionFactoryProvider sessionFactoryProvider;

	private Class<? extends Session> sessionInterface = Session.class;

	private boolean allowCreate = true;

	private Session proxy;

	public void setSessionFactoryProvider(SessionFactoryProvider sessionFactoryProvider) {
		this.sessionFactoryProvider = sessionFactoryProvider;
	}

	protected SessionFactoryProvider getSessionFactoryProvider() {
		return this.sessionFactoryProvider;
	}

	protected Class<? extends Session> getSessionInterface() {
		return this.sessionInterface;
	}

	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	protected boolean isAllowCreate() {
		return this.allowCreate;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactoryProvider() == null) {
			throw new IllegalArgumentException("Property 'sessionFactoryProvider' is required");
		}

		this.proxy = (Session) Proxy.newProxyInstance(
				getSessionFactoryProvider().getClass().getClassLoader(),
				new Class<?>[]{getSessionInterface()}, new SessionInvocationHandler());
	}


	@Override
	public Session getObject() {
		return this.proxy;
	}

	@Override
	public Class<? extends Session> getObjectType() {
		return getSessionInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getSessionFactoryProvider() == null) {
			sessionFactoryProvider = beanFactory.getBean(SessionFactoryProvider.class);
		}
	}

	private class SessionInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on PersistenceManager interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			} else if (method.getName().equals("hashCode")) {
				// Use hashCode of PersistenceManager proxy.
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target EntityManager.
				return "Spring Session proxy for target factory [" + getSessionFactoryProvider() + "]";
			}

			// Invoke method on target PersistenceManager.
			Session session = SessionFactoryProviderUtils.getSession(
					getSessionFactoryProvider(), isAllowCreate());
			try {
				return method.invoke(session, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			} finally {
				SessionFactoryProviderUtils.releaseSession(session, getSessionFactoryProvider());
			}
		}
	}
}
