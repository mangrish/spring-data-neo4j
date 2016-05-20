package org.springframework.data.neo4j.repository.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Created by markangrish on 20/05/2016.
 */
public class Neo4jRepositoryNameSpaceHandler extends NamespaceHandlerSupport {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {

		RepositoryConfigurationExtension extension = new Neo4jRepositoryConfigurationExtension();
		RepositoryBeanDefinitionParser repositoryBeanDefinitionParser = new RepositoryBeanDefinitionParser(extension);

		registerBeanDefinitionParser("repositories", repositoryBeanDefinitionParser);
	}
}
