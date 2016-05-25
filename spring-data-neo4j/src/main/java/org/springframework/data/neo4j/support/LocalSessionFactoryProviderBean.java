package org.springframework.data.neo4j.support;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Created by markangrish on 14/05/2016.
 */
public class LocalSessionFactoryProviderBean implements FactoryBean<SessionFactoryProvider>,
		BeanClassLoaderAware, InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	protected final Log logger = LogFactory.getLog(LocalSessionFactoryProviderBean.class);

	private SessionFactoryProvider sessionFactoryProvider;

	private SessionFactoryProvider nativeSessionFactoryProvider;

	private String[] packagesToScan;

	private ClassLoader beanClassLoader = getClass().getClassLoader();


	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public SessionFactoryProvider getObject() {
		return sessionFactoryProvider;
	}

	@Override
	public Class<? extends SessionFactoryProvider> getObjectType() {
		return sessionFactoryProvider.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() {
		if (logger.isInfoEnabled()) {
			logger.info("Building new Neo4j SessionFactoryProvider");
		}
		this.nativeSessionFactoryProvider = new SessionFactory(packagesToScan);

		// Wrap the SessionFactoryProvider in a factory implementing all its interfaces.
		// This allows interception of createSession methods to return an
		// application-managed Session proxy that automatically joins
		// existing transactions.
		this.sessionFactoryProvider = (SessionFactoryProvider) Proxy.newProxyInstance(
				this.beanClassLoader, new Class<?>[]{SessionFactoryProvider.class},
				new ManagedSessionFactoryInvocationHandler(this));
	}


	/**
	 * Delegate an incoming invocation from the proxy, dispatching to EntityManagerFactoryInfo
	 * or the native EntityManagerFactory accordingly.
	 */
	Object invokeProxyMethod(Method method, Object[] args) throws Throwable {

		// Standard delegation to the native factory, just post-processing EntityManager return values
		return method.invoke(this.nativeSessionFactoryProvider, args);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return SessionFactoryProviderUtils.convertNeo4jAccessExceptionIfPossible(ex);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	private static class ManagedSessionFactoryInvocationHandler implements InvocationHandler, Serializable {

		private final LocalSessionFactoryProviderBean sessionFactoryBean;

		public ManagedSessionFactoryInvocationHandler(LocalSessionFactoryProviderBean lsfpb) {
			this.sessionFactoryBean = lsfpb;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				} else if (method.getName().equals("hashCode")) {
					// Use hashCode of EntityManagerFactory proxy.
					return System.identityHashCode(proxy);
				}

				return this.sessionFactoryBean.invokeProxyMethod(method, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}
}
