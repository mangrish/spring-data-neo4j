package org.springframework.ogm.neo4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Created by markangrish on 14/05/2016.
 */
public class LocalSessionFactoryProviderBean implements FactoryBean<SessionFactoryProvider>,
		InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	protected final Log logger = LogFactory.getLog(LocalSessionFactoryProviderBean.class);

	private SessionFactoryProvider sessionFactoryProvider;
	private String[] packagesToScan;

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public SessionFactoryProvider getObject() {
		return sessionFactoryProvider;
	}

	@Override
	public Class<? extends SessionFactoryProvider> getObjectType() {
		return sessionFactoryProvider.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet()  {
		logger.info("Building new Neo4j SessionFactory");
		this.sessionFactoryProvider = new SessionFactory(packagesToScan);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return SessionFactoryUtils.convertNeo4jAccessExceptionIfPossible(ex);
	}
}
