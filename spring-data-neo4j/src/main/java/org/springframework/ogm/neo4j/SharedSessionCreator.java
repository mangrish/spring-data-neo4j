package org.springframework.ogm.neo4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;

/**
 * Created by markangrish on 19/05/2016.
 */
public class SharedSessionCreator {


	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param emf the SessionFactory to delegate to.
	 * @return a shareable transaction Session proxy
	 */
	public static Session createSharedSession(SessionFactoryProvider emf) {
		return createSharedSession(emf, true);
	}


	/**
	 * Create a transactional EntityManager proxy for the given EntityManagerFactory.
	 *
	 * @param emf the EntityManagerFactory to delegate to.
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return a shareable transaction EntityManager proxy
	 * @since 4.0
	 */
	public static Session createSharedSession(
			SessionFactoryProvider emf, boolean synchronizedWithTransaction) {

		return (Session) Proxy.newProxyInstance(
				(SharedSessionCreator.class.getClassLoader()),
				new Class<?>[]{Session.class}, new SharedSessionInvocationHandler(emf, synchronizedWithTransaction));
	}


	/**
	 * Invocation handler that delegates all calls to the current
	 * transactional EntityManager, if any; else, it will fall back
	 * to a newly created EntityManager per operation.
	 */
	@SuppressWarnings("serial")
	private static class SharedSessionInvocationHandler implements InvocationHandler, Serializable {

		private final Log logger = LogFactory.getLog(getClass());

		private final SessionFactoryProvider targetFactory;

		private final boolean synchronizedWithTransaction;

		private transient volatile ClassLoader proxyClassLoader;

		public SharedSessionInvocationHandler(
				SessionFactoryProvider target, boolean synchronizedWithTransaction) {
			this.targetFactory = target;
			this.synchronizedWithTransaction = synchronizedWithTransaction;
			initProxyClassLoader();
		}

		private void initProxyClassLoader() {
			this.proxyClassLoader = this.targetFactory.getClass().getClassLoader();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on EntityManager interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			} else if (method.getName().equals("hashCode")) {
				// Use hashCode of EntityManager proxy.
				return hashCode();
			} else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target EntityManager.
				return "Shared Session proxy for target factory [" + this.targetFactory + "]";
			} else if (method.getName().equals("metaData")) {
				try {
					return SessionFactoryProvider.class.getMethod(method.getName()).invoke(this.targetFactory);
				} catch (InvocationTargetException ex) {
					throw ex.getTargetException();
				}
			} else if (method.getName().equals("openTransaction")) {
				throw new IllegalStateException(
						"Not allowed to create transaction on shared EntityManager - " +
								"use Spring transactions or EJB CMT instead");
			}

			// Determine current EntityManager: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			Session target = SessionFactoryUtils.getSession(
					this.targetFactory, this.synchronizedWithTransaction);

			if (method.getName().equals("getTargetEntityManager")) {
				// Handle EntityManagerProxy interface.
				if (target == null) {
					throw new IllegalStateException("No transactional EntityManager available");
				}
				return target;
			} else if (method.getName().equals("unwrap")) {
				// We need a transactional target now.
				if (target == null) {
					throw new IllegalStateException("No transactional EntityManager available");
				}
				// Still perform unwrap call on target EntityManager.
			}

			// Invoke method on current EntityManager.
			try {
				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			} finally {
				SessionFactoryUtils.closeSession(target);
			}
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			// Rely on default serialization, just initialize state after deserialization.
			ois.defaultReadObject();
			// Initialize transient fields.
			initProxyClassLoader();
		}
	}
}
