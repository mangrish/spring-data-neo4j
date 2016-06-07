package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.config.Configuration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.session.SessionFactoryImpl;

/**
 * Created by markangrish on 14/05/2016.
 */
public class LocalSessionFactoryBean implements FactoryBean<SessionFactory>,
		InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	protected final Log logger = LogFactory.getLog(LocalSessionFactoryBean.class);

	private SessionFactory sessionFactory;
	private String[] packagesToScan;
	private Configuration configuration;

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public SessionFactory getObject()  {
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
	public void afterPropertiesSet() {
		logger.info("Building new Neo4j SessionFactory");
		if (configuration != null) {
			this.sessionFactory = new SessionFactoryImpl(configuration, packagesToScan);
		} else {
			this.sessionFactory = new SessionFactoryImpl(packagesToScan);
		}
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return Neo4jOgmExceptionTranslator.translateExceptionIfPossible(ex);
	}
}
