package org.springframework.data.neo4j.session;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 27/05/2016.
 */
public interface SessionFactory {

	Session openSession();

	MetaData getMetaData();
}
