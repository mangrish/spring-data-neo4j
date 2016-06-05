package org.springframework.data.neo4j.session;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.support.SpringSessionContext;

/**
 * Created by markangrish on 02/06/2016.
 */
public class SessionFactoryImpl implements SessionFactory {

	private final MetaData metaData;
	private final SessionContext sessionContext;

	public SessionFactoryImpl(String... packages) {
		this.metaData = new MetaData(packages);
		this.sessionContext = buildSessionContext();
	}

	public SessionFactoryImpl(Configuration configuration, String... packages) {
		Components.configure(configuration);
		this.metaData = new MetaData(packages);
		this.sessionContext = buildSessionContext();
	}

	@Override
	public MetaData getMetaData() {
		return metaData;
	}

	@Override
	public Session openSession() {
		return new Neo4jSession(metaData, Components.driver());
	}

	@Override
	public Session getCurrentSession() {
		if (sessionContext == null) {
			throw new RuntimeException("No SessionContext configured!");
		}
		return sessionContext.currentSession();
	}

	@Override
	public void closeSession(Session session) {
		if (sessionContext == null) {
			throw new RuntimeException("No SessionContext configured!");
		}
		sessionContext.closeSession(session);
	}

	public SessionContext buildSessionContext() {

//		if ("thread".equals(impl)) {
//			return new ThreadLocalSessionContext(this);
//		} else {
//
//			Class implClass = ClassUtils.forName(impl, Thread.currentThread().getContextClassLoader());
//			return (SessionContext)
//					implClass.getConstructor(new Class[]{SessionFactoryImpl.class}).newInstance(this);
//		}

		return new SpringSessionContext(this);
	}
}
