package org.springframework.data.neo4j.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.session.SessionFactory;

/**
 * Created by markangrish on 20/05/2016.
 */
public abstract class SharedSessionCreator {

	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param sessionFactory the SessionFactory to delegate to.
	 * @return a shareable transaction Session proxy
	 */
	public static Session createSharedSession(SessionFactory sessionFactory) {
		return createSharedSession(sessionFactory, true);
	}


	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param sessionFactory the SessionFactory to delegate to.
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions
	 * @return a shareable transaction Session proxy
	 * @since 4.0
	 */
	public static Session createSharedSession(
			SessionFactory sessionFactory, boolean synchronizedWithTransaction) {

		return (Session) Proxy.newProxyInstance(
				(SharedSessionCreator.class.getClassLoader()),
				new Class<?>[]{Session.class}, new SharedSessionInvocationHandler(sessionFactory, synchronizedWithTransaction));
	}


	/**
	 * Invocation handler that delegates all calls to the current
	 * transactional Session, if any; else, it will fall back
	 * to a newly created Session per operation.
	 */
	@SuppressWarnings("serial")
	private static class SharedSessionInvocationHandler implements InvocationHandler, Serializable {

		private final SessionFactory targetFactory;

		private final boolean synchronizedWithTransaction;

		private transient volatile ClassLoader proxyClassLoader;

		public SharedSessionInvocationHandler(
				SessionFactory target, boolean synchronizedWithTransaction) {
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
					return SessionFactory.class.getMethod(method.getName()).invoke(this.targetFactory);
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

			if (method.getName().equals("getTargetSession")) {
				// Handle EntityManagerProxy interface.
				if (target == null) {
					throw new IllegalStateException("No transactional Session available");
				}
				return target;
			}
			// Invoke method on current EntityManager.
			try {
				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
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
