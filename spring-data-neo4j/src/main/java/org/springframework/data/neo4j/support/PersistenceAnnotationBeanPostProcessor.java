package org.springframework.data.neo4j.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.data.neo4j.annotation.PersistenceContext;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.support.SharedSessionCreator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Created by markangrish on 31/05/2016.
 */
public class PersistenceAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, Serializable {


	private transient Map<String, String> persistenceContexts;

	private int order = Ordered.LOWEST_PRECEDENCE - 4;

	private transient ListableBeanFactory beanFactory;

	private transient final Map<String, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<String, InjectionMetadata>(256);


	/**
	 * Specify the <i>transactional</i> persistence contexts for Session lookups,
	 * as a Map from persistence unit name to persistence context JNDI name
	 * (which needs to resolve to an Session instance).
	 * <p>JNDI names specified here should refer to {@code persistence-context-ref}
	 * entries in the Java EE deployment descriptors, matching the target persistence unit
	 * and being set up with persistence context type {@code Transaction}.
	 * <p>This is mainly intended for use in a Java EE environment, with all
	 * lookup driven by the standard JPA annotations, and all Session
	 * references obtained from JNDI. No separate SessionFactory bean
	 * definitions are necessary in such a scenario, and all Session
	 * handling is done by the Java EE server itself.
	 */
	public void setPersistenceContexts(Map<String, String> persistenceContexts) {
		this.persistenceContexts = persistenceContexts;
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanType != null) {
			InjectionMetadata metadata = findPersistenceMetadata(beanName, beanType, null);
			metadata.checkConfigMembers(beanDefinition);
		}
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		InjectionMetadata metadata = findPersistenceMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		} catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex);
		}
		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {

	}


	private InjectionMetadata findPersistenceMetadata(String beanName, final Class<?> clazz, PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					try {
						metadata = buildPersistenceMetadata(clazz);
						this.injectionMetadataCache.put(cacheKey, metadata);
					} catch (NoClassDefFoundError err) {
						throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
								"] for persistence metadata: could not find class that it depends on", err);
					}
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildPersistenceMetadata(final Class<?> clazz) {
		LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
		Class<?> targetClass = clazz;

		do {
			final LinkedList<InjectionMetadata.InjectedElement> currElements =
					new LinkedList<InjectionMetadata.InjectedElement>();

			ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					if (field.isAnnotationPresent(PersistenceContext.class)) {
						if (Modifier.isStatic(field.getModifiers())) {
							throw new IllegalStateException("Persistence annotations are not supported on static fields");
						}
						currElements.add(new PersistenceElement(field, field, null));
					}
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
					if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
						return;
					}
					if ((bridgedMethod.isAnnotationPresent(PersistenceContext.class)) &&
							method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("Persistence annotations are not supported on static methods");
						}
						if (method.getParameterTypes().length != 1) {
							throw new IllegalStateException("Persistence annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new PersistenceElement(method, bridgedMethod, pd));
					}
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}


	/**
	 * Find a single default SessionFactory in the Spring application context.
	 *
	 * @return the default SessionFactory
	 * @throws NoSuchBeanDefinitionException if there is no single SessionFactory in the context
	 */
	protected SessionFactory findEntityManagerFactory(String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		String[] beanNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, SessionFactory.class);
		if (beanNames.length == 1) {
			String unitName = beanNames[0];
			SessionFactory emf = (SessionFactory) this.beanFactory.getBean(unitName);
			if (this.beanFactory instanceof ConfigurableBeanFactory) {
				((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(unitName, requestingBeanName);
			}
			return emf;
		} else if (beanNames.length > 1) {
			throw new NoUniqueBeanDefinitionException(SessionFactory.class, beanNames);
		} else {
			throw new NoSuchBeanDefinitionException(SessionFactory.class);
		}
	}


	/**
	 * Class representing injection information about an annotated field
	 * or setter method.
	 */
	private class PersistenceElement extends InjectionMetadata.InjectedElement {

		private boolean synchronizedWithTransaction = true;

		public PersistenceElement(Member member, AnnotatedElement ae, PropertyDescriptor pd) {
			super(member, pd);
			PersistenceContext pc = ae.getAnnotation(PersistenceContext.class);
			Class<?> resourceType = Session.class;
			checkResourceType(resourceType);
		}

		/**
		 * Resolve the object against the application context.
		 */
		@Override
		protected Object getResourceToInject(Object target, String requestingBeanName) {
			// Resolves to SessionFactory or Session.

			// OK, so we need an Sessio...
			return resolveEntityManager(requestingBeanName);
		}

		private Session resolveEntityManager(String requestingBeanName) {
			SessionFactory emf = findEntityManagerFactory(requestingBeanName);
			Session em = SharedSessionCreator.createSharedSession(
					emf, this.synchronizedWithTransaction);
			return em;
		}
	}
}
