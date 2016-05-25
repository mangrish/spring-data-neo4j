package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.session.Session;

/**
 * Subinterface of {@link Session} to be implemented by
 * Session proxies. Allows access to the underlying target Session.
 * <p>This interface is mainly intended for framework usage.
 *
 * @author Mark Angrish
 */
public interface SessionProxy extends Session {

	/**
	 * Return the underlying Session that this proxy will delegate to.
	 * <p>In case of a shared ("transactional") Session, this will be
	 * the raw Session that is currently associated with the transaction.
	 * Outside of a transaction, an IllegalStateException will be thrown.
	 *
	 * @return the underlying raw Session (never {@code null})
	 * @throws IllegalStateException if no underlying Session is available
	 */
	Session getTargetSession() throws IllegalStateException;
}
