package org.springframework.ogm.neo4j;

import org.neo4j.ogm.session.Session;

/**
 * Created by markangrish on 19/05/2016.
 */
public interface SessionProxy extends Session {

	Session getTargetSession() throws IllegalStateException;
}
