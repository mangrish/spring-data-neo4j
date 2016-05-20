package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;

/**
 * Created by markangrish on 19/05/2016.
 */
public class Neo4jMappingContextFactoryBean extends AbstractFactoryBean<Neo4jMappingContext> implements
		ApplicationContextAware {

	private ApplicationContext beanFactory;


	@Override
	public Class<?> getObjectType() {
		return Neo4jMappingContext.class;
	}

	@Override
	protected Neo4jMappingContext createInstance() throws Exception {

		SessionFactoryProvider sessionFactory = beanFactory.getBean(SessionFactoryProvider.class);
		Neo4jMappingContext context = new Neo4jMappingContext(sessionFactory.metaData());
		context.initialize();

		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}
}
