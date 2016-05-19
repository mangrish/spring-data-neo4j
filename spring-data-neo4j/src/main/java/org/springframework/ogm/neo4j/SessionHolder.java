package org.springframework.ogm.neo4j;

import org.neo4j.ogm.session.Session;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Created by markangrish on 12/05/2016.
 */
public class SessionHolder extends ResourceHolderSupport {

	private final Session session;

	private boolean transactionActive;

	public SessionHolder(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}


	public Session getSession() {
		return this.session;
	}

	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	protected boolean isTransactionActive() {
		return this.transactionActive;
	}


	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}
}
