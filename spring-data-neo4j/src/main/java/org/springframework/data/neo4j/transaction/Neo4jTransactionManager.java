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

import java.util.List;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * This class is a wrapper around the OGM TransactionManager.
 * <p>In order to support Transaction event synchronization this class has had part of the functionality from
 * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager} merged into it.  Normally this
 * class would be extended but Neo4J's transactional behaviour is not as sophisticated as the generic case that the
 * <code>AbstractPlatformTransactionManager</code> needs to facilitate.</p>
 *
 * @author Vince Bickers
 * @author Mark Angrish
 */
public class Neo4jTransactionManager implements PlatformTransactionManager {

	private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);
	private final Session session;

	public Neo4jTransactionManager(Session session) {
		this.session = session;
	}

	@Override
	public final TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {

		Transaction transaction = doGetTransaction();
		boolean debugEnabled = this.logger.isDebugEnabled();
		if (definition == null) {
			definition = new DefaultTransactionDefinition();
		}

		if (this.isExistingTransaction(transaction)) {
			return this.handleExistingTransaction(definition, transaction, debugEnabled);
		} else {
			SuspendedResourcesHolder newSynchronization = this.suspend(null);
			if (debugEnabled) {
				this.logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
			}

			try {
				transaction = doBegin(transaction, definition);
				Neo4jTransactionStatus status = this.newTransactionStatus(definition, transaction, true, true, debugEnabled, newSynchronization);
				this.prepareSynchronization(status, definition);
				return status;
			} catch (RuntimeException ex) {
				this.resume(null, newSynchronization);
				throw ex;
			}
		}
	}

	protected Transaction doBegin(Transaction transaction, TransactionDefinition definition) {
		return session.beginTransaction();
	}

	protected Transaction doGetTransaction() {
		return session.getTransaction();
	}


	private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Transaction transaction, boolean debugEnabled) throws TransactionException {

		SuspendedResourcesHolder newSynchronization;
		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				this.logger.debug("Suspending current transaction, creating new transaction with name [" + definition.getName() + "]");
			}

			newSynchronization = this.suspend(transaction);

			try {
				Neo4jTransactionStatus status = this.newTransactionStatus(definition, transaction, true, true, debugEnabled, newSynchronization);
				transaction = doBegin(transaction, definition);
				this.prepareSynchronization(status, definition);
				return status;
			} catch (RuntimeException ex) {
				this.resumeAfterBeginException(transaction, newSynchronization, ex);
				throw ex;
			}
		} else if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED) {

			if (debugEnabled) {
				this.logger.debug("Participating in existing transaction");
			}

			return this.prepareTransactionStatus(definition, transaction, false, true, debugEnabled, null);
		} else {
			throw new RuntimeException("Transaction propagation type not supported: " + definition.getPropagationBehavior());
		}
	}

	private boolean isExistingTransaction(Transaction transaction) {
		return !(transaction == null
				|| transaction.status().equals(Transaction.Status.CLOSED)
				|| transaction.status().equals(Transaction.Status.COMMITTED)
				|| transaction.status().equals(Transaction.Status.ROLLEDBACK));
	}


	private Neo4jTransactionStatus prepareTransactionStatus(TransactionDefinition definition, Transaction transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {
		Neo4jTransactionStatus status = this.newTransactionStatus(definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
		this.prepareSynchronization(status, definition);
		return status;
	}

	private Neo4jTransactionStatus newTransactionStatus(TransactionDefinition definition, Transaction transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {
		boolean actualNewSynchronization = newSynchronization && !TransactionSynchronizationManager.isSynchronizationActive();
		return new Neo4jTransactionStatus(transaction, newTransaction, actualNewSynchronization, definition.isReadOnly(), debug, suspendedResources);
	}

	private void prepareSynchronization(Neo4jTransactionStatus status, TransactionDefinition definition) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
			TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
			TransactionSynchronizationManager.initSynchronization();
		}
	}


	private SuspendedResourcesHolder suspend(Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			List<TransactionSynchronization> suspendSynchronizations = this.doSuspendSynchronization();

			try {
				if (transaction != null) {
					throw new TransactionSuspensionNotSupportedException("Transaction manager [" + this.getClass().getName() + "] does not support transaction suspension");
				}

				String name = TransactionSynchronizationManager.getCurrentTransactionName();
				TransactionSynchronizationManager.setCurrentTransactionName(null);
				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
				TransactionSynchronizationManager.setActualTransactionActive(false);
				return new SuspendedResourcesHolder(null, suspendSynchronizations, name, readOnly, wasActive);
			} catch (RuntimeException var8) {
				this.doResumeSynchronization(suspendSynchronizations);
				throw var8;
			}
		} else if (transaction != null) {
			throw new TransactionSuspensionNotSupportedException("Transaction manager [" + this.getClass().getName() + "] does not support transaction suspension");
		} else {
			return null;
		}
	}

	private void resume(Object transaction, SuspendedResourcesHolder resourcesHolder) throws TransactionException {
		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				this.doResume(transaction, suspendedResources);
			}

			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				this.doResumeSynchronization(suspendedSynchronizations);
			}
		}
	}

	private void resumeAfterBeginException(Object transaction, SuspendedResourcesHolder suspendedResources, Throwable beginEx) {
		String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";

		try {
			this.resume(transaction, suspendedResources);
		} catch (RuntimeException var6) {
			this.logger.error(exMessage, beginEx);
			throw var6;
		}
	}


	private List<TransactionSynchronization> doSuspendSynchronization() {
		List<TransactionSynchronization> suspendedSynchronizations = TransactionSynchronizationManager.getSynchronizations();

		for (Object suspendedSynchronization : suspendedSynchronizations) {
			TransactionSynchronization synchronization = (TransactionSynchronization) suspendedSynchronization;
			synchronization.suspend();
		}

		TransactionSynchronizationManager.clearSynchronization();
		return suspendedSynchronizations;
	}

	private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
		TransactionSynchronizationManager.initSynchronization();

		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.resume();
			TransactionSynchronizationManager.registerSynchronization(synchronization);
		}
	}

	private void doResume(Object transaction, Object suspendedResources) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException("Transaction manager [" + this.getClass().getName() + "] does not support transaction suspension");
	}


	@Override
	public void commit(TransactionStatus transactionStatus) throws TransactionException {

		if (transactionStatus.isCompleted()) {
			throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
		} else {
			Neo4jTransactionStatus defStatus = (Neo4jTransactionStatus) transactionStatus;
			this.processCommit(defStatus);
		}
	}


	private void processCommit(Neo4jTransactionStatus status) throws TransactionException {
		try {
			boolean beforeCompletionInvoked = false;

			try {
				this.triggerBeforeCommit(status);
				this.triggerBeforeCompletion(status);
				beforeCompletionInvoked = true;

				if (status.isNewTransaction()) {
					if (status.isDebug()) {
						this.logger.debug("Initiating transaction commit");
					}

					this.doCommit(status);
				}
			} catch (UnexpectedRollbackException var19) {
				this.triggerAfterCompletion(status, 1);
				throw var19;
			} catch (TransactionException var20) {

				this.triggerAfterCompletion(status, 2);

				throw var20;
			} catch (RuntimeException var21) {
				if (!beforeCompletionInvoked) {
					this.triggerBeforeCompletion(status);
				}

				throw var21;
			}

			try {
				this.triggerAfterCommit(status);
			} finally {
				this.triggerAfterCompletion(status, 0);
			}
		} finally {
			this.cleanupAfterCompletion(status);
		}
	}

	protected void doCommit(Neo4jTransactionStatus status) {
		Transaction tx = status.getTransaction();
		if (status.isNewTransaction() && canCommit(tx)) {
			logger.debug("Commit requested: " + tx + ", status: " + tx.status().toString());
			tx.commit();
			tx.close();
		}
	}

	private void triggerBeforeCommit(Neo4jTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				this.logger.trace("Triggering beforeCommit synchronization");
			}

			TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
		}
	}

	private void triggerBeforeCompletion(Neo4jTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				this.logger.trace("Triggering beforeCompletion synchronization");
			}

			TransactionSynchronizationUtils.triggerBeforeCompletion();
		}
	}

	private void triggerAfterCommit(Neo4jTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				this.logger.trace("Triggering afterCommit synchronization");
			}

			TransactionSynchronizationUtils.triggerAfterCommit();
		}
	}

	private void triggerAfterCompletion(Neo4jTransactionStatus status, int completionStatus) {
		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			if (status.hasTransaction() && !status.isNewTransaction()) {
				if (!synchronizations.isEmpty()) {
					this.registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
				}
			} else {
				if (status.isDebug()) {
					this.logger.trace("Triggering afterCompletion synchronization");
				}

				TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
			}
		}
	}

	private void cleanupAfterCompletion(Neo4jTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.clear();
		}

		if (status.getSuspendedResources() != null) {
			if (status.isDebug()) {
				this.logger.debug("Resuming suspended transaction after completion of inner transaction");
			}

			this.resume(status.getTransaction(), (SuspendedResourcesHolder) status.getSuspendedResources());
		}
	}

	private void registerAfterCompletionWithExistingTransaction(Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {
		this.logger.debug("Cannot register Spring after-completion synchronization with existing transaction - processing Spring after-completion callbacks immediately, with outcome status \'unknown\'");
		TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, 2);
	}


	@Override
	public final void rollback(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
		} else {
			this.processRollback((Neo4jTransactionStatus) status);
		}
	}

	private void processRollback(Neo4jTransactionStatus status) {
		try {
			try {
				this.triggerBeforeCompletion(status);
				if (status.isNewTransaction()) {
					if (status.isDebug()) {
						this.logger.debug("Initiating transaction rollback");
					}

					this.doRollback(status);
				} else if (status.hasTransaction()) {
					if (status.isDebug()) {
						this.logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
					}
					status.setRollbackOnly();
				} else {
					this.logger.debug("Should roll back transaction but cannot - no transaction available");
				}
			} catch (RuntimeException var7) {
				this.triggerAfterCompletion(status, 2);
				throw var7;
			}

			this.triggerAfterCompletion(status, 1);
		} finally {
			this.cleanupAfterCompletion(status);
		}
	}

	protected void doRollback(Neo4jTransactionStatus transactionStatus) throws TransactionException {
		Transaction tx = transactionStatus.getTransaction();
		if (transactionStatus.isNewTransaction() && canRollback(tx)) {
			logger.debug("Rollback requested: " + tx + ", status: " + tx.status().toString());
			tx.rollback();
			tx.close();
		}
	}


	private boolean canCommit(Transaction tx) {
		switch (tx.status()) {
			case COMMIT_PENDING:
				return true;
			case OPEN:
				return true;
			default:
				return false;
		}
	}

	private boolean canRollback(Transaction tx) {
		switch (tx.status()) {
			case OPEN:
				return true;
			case ROLLBACK_PENDING:
				return true;
			default:
				return false;
		}
	}

	private static class SuspendedResourcesHolder {

		private final Object suspendedResources;
		private List<TransactionSynchronization> suspendedSynchronizations;
		private String name;
		private boolean readOnly;
		private boolean wasActive;

		private SuspendedResourcesHolder(Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations, String name, boolean readOnly, boolean wasActive) {
			this.suspendedResources = suspendedResources;
			this.suspendedSynchronizations = suspendedSynchronizations;
			this.name = name;
			this.readOnly = readOnly;
			this.wasActive = wasActive;
		}
	}
}
