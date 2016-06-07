package org.springframework.data.neo4j.transactions;


import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.data.neo4j.session.SessionFactory;
import org.springframework.data.neo4j.session.SessionFactoryImpl;
import org.springframework.data.neo4j.support.LocalSessionFactoryBean;
import org.springframework.data.neo4j.support.SpringSessionContext;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 3.2
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Neo4jTransactionManagerTests {

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testTransactionCommit() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);
		Result query = mock(Result.class);

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.getTransaction()).willReturn(tx);
		given(session.query("some query string", new HashMap<String, Object>())).willReturn(query);
		given(query.queryResults()).willReturn(list);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sf);
		final SessionFactory sfProxy = lsfb.getObject();

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sfProxy));
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sfProxy)).getSession();
				return session.query("some query string", new HashMap<String, Object>()).queryResults();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}

	@Test
	public void testTransactionRollback() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new Neo4jTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					throw new RuntimeException("application exception");
				}
			});
			fail("Should have thrown RuntimeException");
		} catch (RuntimeException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(tx).rollback();
	}

	@Test
	public void testTransactionRollbackOnly() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new Neo4jTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				status.setRollbackOnly();
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithCommit() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sf);
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new Neo4jTransactionManager(sfProxy);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		verify(tx).commit();
	}

	@Test
	public void testParticipatingTransactionWithRollback() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new Neo4jTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							throw new RuntimeException("application exception");
						}
					});
				}
			});
			fail("Should have thrown RuntimeException");
		} catch (RuntimeException ex) {
			// expected
		}

		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new Neo4jTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							status.setRollbackOnly();
							return null;
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		} catch (UnexpectedRollbackException ex) {
			// expected
		}

		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNew() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session1, session2);
		given(session1.beginTransaction()).willReturn(tx);
		given(session2.beginTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new Neo4jTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						assertTrue("Not enclosing session", session != holder.getSession());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						return null;
					}
				});
				assertTrue("Same thread session as before",
						holder.getSession() == ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(tx, times(2)).commit();
	}

	@Test
	public void testParticipatingTransactionWithNotSupported() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		Neo4jTransactionManager tm = new Neo4jTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
				tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						return null;
					}
				});
				assertTrue("Same thread session as before",
						holder.getSession() == ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(tx).commit();
	}

	@Test
	public void testTransactionWithPropagationSupports() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sf);

		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new Neo4jTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = sf.openSession();
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		InOrder ordered = inOrder(session);
	}

	@Test
	public void testTransactionWithPropagationSupportsAndCurrentSession() throws Exception {
		final SessionFactoryImpl sf = mock(SessionFactoryImpl.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sf);

		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new Neo4jTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = new SpringSessionContext(sf).currentSession();
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sfProxy));
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		InOrder ordered = inOrder(session);
	}

	@Test
	public void testTransactionWithPropagationSupportsAndInnerTransaction() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session1 = mock(Session.class);
		final Session session2 = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session1, session2);
		given(session2.beginTransaction()).willReturn(tx);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sf);
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new Neo4jTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = sfProxy.openSession();
				assertSame(session1, session);
				tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						assertSame(session2, session);
						return null;
					}
				});

				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(tx).commit();
	}


	@Test
	public void testTransactionCommitWithReadOnly() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		Neo4jTransactionManager tm = new Neo4jTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setReadOnly(true);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				return session.query("some query string", new HashMap<String, Object>()).queryResults();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}


	@Test
	public void testTransactionCommitWithPreBound() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx);

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread transaction", sessionHolder.getTransaction() != null);
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithPreBoundAndResultAccessAfterCommit() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx);

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread transaction", sessionHolder.getTransaction() != null);
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}

	@Test
	public void testTransactionRollbackWithPreBound() throws Exception {

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		final Transaction tx1 = mock(Transaction.class);
		final Transaction tx2 = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx1, tx2);

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertEquals(tx1, sessionHolder.getTransaction());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						public void doInTransactionWithoutResult(TransactionStatus status) {
							status.setRollbackOnly();
							Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
							assertEquals(session, sess);
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		} catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		assertTrue("Not marked rollback-only", !sessionHolder.isRollbackOnly());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertEquals(tx2, sessionHolder.getTransaction());
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
			}
		});

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx1).rollback();
		verify(tx2).commit();
		InOrder ordered = inOrder(session);
		ordered.verify(session).clear();
	}

	@Test
	public void testTransactionRollbackWithNeo4jOgmManagedSession() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		final Transaction tx1 = mock(Transaction.class);
		final Transaction tx2 = mock(Transaction.class);

		given(sf.getCurrentSession()).willReturn(session);
		given(session.getTransaction()).willReturn(tx1, tx2);
		given(session.beginTransaction()).willReturn(tx1, tx2);

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						public void doInTransactionWithoutResult(TransactionStatus status) {
							status.setRollbackOnly();
							Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
							assertEquals(session, sess);
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		} catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
			}
		});

		verify(tx1).rollback();
		verify(tx2).commit();
		InOrder ordered = inOrder(session);
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
		doTestExistingTransactionWithPropagationNestedAndRollback(false);
	}


	private void doTestExistingTransactionWithPropagationNestedAndRollback(final boolean manualSavepoint)
			throws Exception {

		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);
		Result query = mock(Result.class);

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.query("some query string", new HashMap<String, Object>())).willReturn(query);
		given(query.queryResults()).willReturn(list);

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setNestedTransactionAllowed(true);
		tm.setSessionFactory(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				if (manualSavepoint) {
					Object savepoint = status.createSavepoint();
					status.rollbackToSavepoint(savepoint);
				} else {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							status.setRollbackOnly();
						}
					});
				}
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				return session.query("some query string", new HashMap<String, Object>()).queryResults();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithNonExistingDatabase() throws Exception {
		final SessionFactory sfi = mock(SessionFactory.class);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setSessionFactory(sfi);
		final SessionFactory sf = lsfb.getObject();

		Neo4jTransactionManager tm = new Neo4jTransactionManager();
		tm.setSessionFactory(sf);
		tm.afterPropertiesSet();
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
					return session.query("from java.lang.Object", new HashMap<String, Object>()).queryResults();
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		} catch (CannotCreateTransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testTransactionFlush() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		Neo4jTransactionManager tm = new Neo4jTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				status.flush();
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx).commit();
	}
}
