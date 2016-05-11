/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 */
public class Neo4jTransactionStatus implements TransactionStatus {

	private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionStatus.class);

	private final Transaction transaction;
	private final boolean readOnly;
	private boolean newTransaction = false;
	private boolean debug = false;
	private boolean newSynchronization;
	private Object suspendedResources;

	Neo4jTransactionStatus(Transaction transaction, boolean newTransaction, boolean newSynchronization, boolean readOnly, boolean debug, Object suspendedResources) {

		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.debug = debug;
		this.newSynchronization = newSynchronization;
		this.readOnly = readOnly;
		this.suspendedResources = suspendedResources;
	}

	@Override
	public boolean isNewTransaction() {
		logger.debug("isNewTransaction? " + newTransaction);
		return newTransaction;
	}

	@Override
	public boolean hasSavepoint() {
		logger.info("hasSavePoint? - false");
		return false;
	}

	@Override
	public void setRollbackOnly() {
		logger.info("setRollbackOnly - unsupported");
	}

	@Override
	public boolean isRollbackOnly() {
		logger.info("isRollbackOnly? - false");
		return false;
	}

	@Override
	public void flush() {
		logger.info("flush - unsupported");
	}

	@Override
	public boolean isCompleted() {
		boolean completed = transaction.status().equals(Transaction.Status.ROLLEDBACK) ||
				transaction.status().equals(Transaction.Status.COMMITTED) ||
				transaction.status().equals(Transaction.Status.CLOSED);

		logger.debug("isCompleted? " + completed);

		return completed;
	}

	@Override
	public Object createSavepoint() throws TransactionException {
		logger.info("createSavepoint - unsupported");
		return null;
	}

	@Override
	public void rollbackToSavepoint(Object o) throws TransactionException {
		logger.info("rollbackToSavepoint - unsupported");
	}

	@Override
	public void releaseSavepoint(Object o) throws TransactionException {
		logger.info("releaseSavepoint - unsupported");
	}

	public Transaction getTransaction() {
		return transaction;
	}

	boolean isDebug() {
		return debug;
	}

	boolean isNewSynchronization() {
		return newSynchronization;
	}

	boolean hasTransaction() {
		return transaction != null;
	}

	boolean isReadOnly() {
		return readOnly;
	}

	Object getSuspendedResources() {
		return suspendedResources;
	}
}
