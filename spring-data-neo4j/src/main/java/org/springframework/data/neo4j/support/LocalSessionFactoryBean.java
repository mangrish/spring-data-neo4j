package org.springframework.data.neo4j.support;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.session.Neo4jSessionFactory;
import org.springframework.data.neo4j.session.SessionFactory;

/**
 * Created by markangrish on 14/05/2016.
 */
public class LocalSessionFactoryBean implements FactoryBean<SessionFactory>,
		BeanClassLoaderAware, PersistenceExceptionTranslator, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(LocalSessionFactoryBean.class);

	private SessionFactory sessionFactory;

	private SessionFactory nativeSessionFactory;

	private String[] packagesToScan;

	private ClassLoader beanClassLoader = getClass().getClassLoader();

	private Configuration config;

	public void setConfiguration(Configuration configuration) {
		this.config = configuration;
	}

	public final Configuration getConfiguration() {
		if (this.config == null) {
			throw new IllegalStateException("Configuration not initialized yet");
		}
		return this.config;
	}

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public SessionFactory getObject() {
		return sessionFactory;
	}

	@Override
	public Class<? extends SessionFactory> getObjectType() {
		return sessionFactory.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return SessionFactoryUtils.convertNeo4jAccessExceptionIfPossible(ex);
	}

	@Override
	public void afterPropertiesSet() {
		if (logger.isInfoEnabled()) {
			logger.info("Building new Neo4j SessionFactory");
		}

		if (config == null) {
			this.nativeSessionFactory = new Neo4jSessionFactory(packagesToScan);
		} else {
			this.nativeSessionFactory = new Neo4jSessionFactory(config, packagesToScan);
		}

		// Wrap the SessionFactory in a factory implementing all its interfaces.
		// This allows interception of createSession methods to return an
		// application-managed Session proxy that automatically joins
		// existing transactions.
		this.sessionFactory = (SessionFactory) Proxy.newProxyInstance(
				this.beanClassLoader, new Class<?>[]{SessionFactory.class},
				new ManagedSessionFactoryInvocationHandler(this));
	}


	/**
	 * Delegate an incoming invocation from the proxy, dispatching to EntityManagerFactoryInfo
	 * or the native EntityManagerFactory accordingly.
	 */
	Object invokeProxyMethod(Method method, Object[] args) throws Throwable {

		 if (method.getName().equals("openSession")) {
			// JPA 2.1's createEntityManager(SynchronizationType, Map)
			// Redirect to plain createEntityManager and add synchronization semantics through Spring proxy
			Session rawEntityManager = getNativeSessionFactory().openSession();
			return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
		}

		// Standard delegation to the native factory, just post-processing EntityManager return values
		Object retVal =  method.invoke(this.nativeSessionFactory, args);

		if (retVal instanceof Session) {
			// Any other createEntityManager variant - expecting non-synchronized semantics
			Session rawEntityManager = (Session) retVal;
			retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
		}
		return retVal;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	public SessionFactory getNativeSessionFactory() {
		return this.nativeSessionFactory;
	}



	private static class ManagedSessionFactoryInvocationHandler implements InvocationHandler, Serializable {

		private final LocalSessionFactoryBean sessionFactoryBean;

		public ManagedSessionFactoryInvocationHandler(LocalSessionFactoryBean lsfpb) {
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
