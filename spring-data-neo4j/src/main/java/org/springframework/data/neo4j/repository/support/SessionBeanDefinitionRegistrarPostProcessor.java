package org.springframework.data.neo4j.repository.support;

import static org.springframework.beans.factory.BeanFactoryUtils.transformedBeanName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.ogm.neo4j.LocalSessionFactoryProviderBean;

/**
 * Created by markangrish on 19/05/2016.
 */
public class SessionBeanDefinitionRegistrarPostProcessor  implements BeanFactoryPostProcessor {

	private static final List<Class<?>> SFP_TYPES;

	static {

		List<Class<?>> types = new ArrayList<Class<?>>();
		types.add(SessionFactoryProvider.class);
		types.add(LocalSessionFactoryProviderBean.class);

		SFP_TYPES = Collections.unmodifiableList(types);
	}


	public static Collection<SessionFactoryProviderBeanDefinition> getSessionFactoryProviderBeanDefinitions(
			ConfigurableListableBeanFactory beanFactory) {

		List<SessionFactoryProviderBeanDefinition> definitions = new ArrayList<>();

		for (Class<?> type : SFP_TYPES) {

			for (String name : beanFactory.getBeanNamesForType(type, true, false)) {
				definitions.add(new SessionFactoryProviderBeanDefinition(transformedBeanName(name), beanFactory));
			}
		}

		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

		if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
			definitions.addAll(getSessionFactoryProviderBeanDefinitions((ConfigurableListableBeanFactory) parentBeanFactory));
		}

		return definitions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		for (SessionFactoryProviderBeanDefinition definition : getSessionFactoryProviderBeanDefinitions(beanFactory)) {

			if (!(definition.getBeanFactory() instanceof BeanDefinitionRegistry)) {
				continue;
			}

			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.rootBeanDefinition("org.springframework.ogm.neo4j.SharedSessionCreator");
			builder.setFactoryMethod("createSharedSession");
			builder.addConstructorArgReference(definition.getBeanName());

			AbstractBeanDefinition emBeanDefinition = builder.getRawBeanDefinition();

			emBeanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, definition.getBeanName()));
			emBeanDefinition.setScope(definition.getBeanDefinition().getScope());
			emBeanDefinition.setSource(definition.getBeanDefinition().getSource());

			BeanDefinitionReaderUtils.registerWithGeneratedName(emBeanDefinition,
					(BeanDefinitionRegistry) definition.getBeanFactory());
		}
	}
}
