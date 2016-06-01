/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.config;

import java.util.Collection;
import java.util.Collections;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.support.GraphRepositoryFactoryBean;
import org.springframework.data.neo4j.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.data.neo4j.repository.support.SessionBeanDefinitionRegistrarPostProcessor;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.util.StringUtils;

/**
 * @author Vince Bickers
 */
public class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	private static final Class<?> PAB_POST_PROCESSOR = PersistenceAnnotationBeanPostProcessor.class;

	public static final String SESSION_BEAN_DEFINITION_REGISTRAR_POST_PROCESSOR_BEAN_NAME = "sBeanDefinitionRegistrarPostProcessor";
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";
	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	public static final String NEO4J_MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";

	@Override
	public String getRepositoryFactoryClassName() {
		return GraphRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return "neo4j";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingTypes()
	 */
	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.<Class<?>>singleton(GraphRepository.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		String transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef == null ? DEFAULT_TRANSACTION_MANAGER_BEAN_NAME : transactionManagerRef);
		builder.addPropertyValue("session", getSessionBeanDefinitionFor(source, source.getSource()));
		builder.addPropertyReference("mappingContext", NEO4J_MAPPING_CONTEXT_BEAN_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();

		builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
				attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		String enableDefaultTransactions = config.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);

		if (StringUtils.hasText(enableDefaultTransactions)) {
			builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, enableDefaultTransactions);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();

		registerIfNotAlreadyRegistered(new RootBeanDefinition(SessionBeanDefinitionRegistrarPostProcessor.class),
				registry, SESSION_BEAN_DEFINITION_REGISTRAR_POST_PROCESSOR_BEAN_NAME, source);

		registerIfNotAlreadyRegistered(new RootBeanDefinition(Neo4jMappingContextFactoryBean.class), registry,
				NEO4J_MAPPING_CONTEXT_BEAN_NAME, source);

		registerIfNotAlreadyRegistered(new RootBeanDefinition(PAB_POST_PROCESSOR), registry,
				AnnotationConfigUtils.PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME, source);
	}

	/**
	 * Creates an anonymous factory to extract the actual {@link Session} from the
	 * {@link SessionFactory} bean name reference.
	 *
	 * @param source
	 * @return
	 */
	private static AbstractBeanDefinition getSessionBeanDefinitionFor(RepositoryConfigurationSource config,
																	  Object source) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.data.neo4j.support.SharedSessionCreator");
		builder.setFactoryMethod("createSharedSession");
		builder.addConstructorArgReference(getSessionFactoryBeanRef(config));

		AbstractBeanDefinition bean = builder.getRawBeanDefinition();
		bean.setSource(source);

		return bean;
	}

	private static String getSessionFactoryBeanRef(RepositoryConfigurationSource config) {

		String sessionFactoryRef = config == null ? null : config.getAttribute("sessionFactoryRef");
		return sessionFactoryRef == null ? "sessionFactory" : sessionFactoryRef;
	}
}
