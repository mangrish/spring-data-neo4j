package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Created by markangrish on 19/05/2016.
 */
public class SessionFactoryProviderBeanDefinition {

	private final String beanName;
	private final ConfigurableListableBeanFactory beanFactory;

	/**
	 * Creates a new {@link SessionFactoryProviderBeanDefinition}.
	 *
	 * @param beanName
	 * @param beanFactory
	 */
	public SessionFactoryProviderBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {

		this.beanName = beanName;
		this.beanFactory = beanFactory;
	}

	/**
	 * Returns the bean name of the {@link BeanDefinition} for the {@link SessionFactoryProvider}.
	 *
	 * @return
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Returns the underlying {@link BeanFactory}.
	 *
	 * @return
	 */
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	/**
	 * Returns the {@link BeanDefinition} for the {@link SessionFactoryProvider}.
	 *
	 * @return
	 */
	public BeanDefinition getBeanDefinition() {
		return beanFactory.getBeanDefinition(beanName);
	}
}
