package org.springframework.data.neo4j.session;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 27/05/2016.
 */
public class Neo4jSessionFactory implements SessionFactory {


	private final MetaData metaData;

	/**
	 * Constructs a new {@link org.neo4j.ogm.session.SessionFactory} by initialising the object-graph mapping meta-data from the given list of domain
	 * object packages.
	 * <p>
	 * The package names passed to this constructor should not contain wildcards or trailing full stops, for example,
	 * "org.springframework.data.neo4j.example.domain" would be fine.  The default behaviour is for sub-packages to be scanned
	 * and you can also specify fully-qualified class names if you want to cherry pick particular classes.
	 * </p>
	 *
	 * @param packages The packages to scan for domain objects
	 */
	public Neo4jSessionFactory(String... packages) {
		this.metaData = new MetaData(packages);
	}

	/**
	 * Constructs a new {@link org.neo4j.ogm.session.SessionFactory} by initialising the object-graph mapping meta-data from the given list of domain
	 * object packages, and also sets the configuration to be used.
	 * <p>
	 * The package names passed to this constructor should not contain wildcards or trailing full stops, for example,
	 * "org.springframework.data.neo4j.example.domain" would be fine.  The default behaviour is for sub-packages to be scanned
	 * and you can also specify fully-qualified class names if you want to cherry pick particular classes.
	 * </p>
	 *
	 * @param packages The packages to scan for domain objects
	 */
	public Neo4jSessionFactory(Configuration configuration, String... packages) {
		Components.configure(configuration);
		this.metaData = new MetaData(packages);
	}

	/**
	 * Retrieves the meta-data that was built up when this {@link org.neo4j.ogm.session.SessionFactory} was constructed.
	 *
	 * @return The underlying {@link MetaData}
	 */
	public MetaData getMetaData() {
		return metaData;
	}

	/**
	 * Opens a new Neo4j mapping {@link Session} using the Driver specified in the OGM configuration
	 * The driver should be configured to connect to the database using the appropriate
	 * DriverConfig
	 *
	 * @return A new {@link Session}
	 */
	public Session openSession() {
		return new Neo4jSession(metaData, Components.driver());
	}
}
