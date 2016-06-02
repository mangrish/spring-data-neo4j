package org.springframework.data.neo4j.session;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 02/06/2016.
 */
public class SessionFactoryImpl implements SessionFactory {

	private final ThreadLocal<Session> sessions = new ThreadLocal<>();
	private final MetaData metaData;

	public SessionFactoryImpl(String... packages) {
		this.metaData = new MetaData(packages);
	}

	public SessionFactoryImpl(Configuration configuration, String... packages) {
		Components.configure(configuration);
		this.metaData = new MetaData(packages);
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

		Session currentSession = sessions.get();

		if (currentSession == null) {
			currentSession = openSession();
			sessions.set(currentSession);
		}

		return currentSession;
	}

	@Override
	public void closeSession(Session session) {
		Session currentSession = sessions.get();

		if (currentSession != null) {
			sessions.remove();
		}
	}
}
