package org.springframework.data.neo4j.repository.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Simple namespace handler for {@literal repositories} namespace.
 *
 * @author Mark Angrish
 */
public class Neo4jRepositoryNamespaceHandler extends NamespaceHandlerSupport {

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
