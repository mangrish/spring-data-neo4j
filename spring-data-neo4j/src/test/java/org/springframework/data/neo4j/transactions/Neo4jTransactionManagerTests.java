package org.springframework.data.neo4j.transactions;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.data.neo4j.transaction.LocalSessionFactoryBean;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.transaction.SessionFactoryUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.data.neo4j.transaction.support.SpringSessionProxyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Created by markangrish on 14/05/2016.
 */
public class Neo4jTransactionManagerTests {

	private SessionFactory factory;

	private Session session;

	private Transaction tx;
//
//	@Before
//	public void setUp() {
//		factory = mock(SessionFactory.class);
//		session = mock(Session.class);
//		tx = mock(Transaction.class);
//	}
//
//	@After
//	public void tearDown() {
//		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
//		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
//		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
//		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
//	}
//
//	@Test
//	public void testTransactionCommit()  {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		final List<String> l = new ArrayList<>();
//		l.add("test");
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//
//				LocalSessionFactoryBean proxyFactory =
//						new LocalSessionFactoryBean();
////				proxyFactory.setTargetPersistenceManagerFactory(factory);
//				SessionFactory pmfProxy = null;
//				try {
//					pmfProxy = proxyFactory.getObject();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				assertEquals(session.toString(), pmfProxy.openSession().toString());
//
//				SpringSessionProxyBean proxyBean = new SpringSessionProxyBean();
//				proxyBean.setSessionFactory(factory);
//				proxyBean.afterPropertiesSet();
//				Session pmProxy = proxyBean.getObject();
////				assertSame(factory, pmProxy.getSessionFactory());
//
////
////				StandardPersistenceManagerProxyBean stdProxyBean = new StandardPersistenceManagerProxyBean();
////				stdProxyBean.setPersistenceManagerFactory(factory);
////				PersistenceManager stdPmProxy = stdProxyBean.getObject();
////				stdPmProxy.flush();
//
////				SessionFactoryUtils.getSession(factory, true).flush();
//				return l;
//			}
//		});
//		assertTrue("Correct result list", result == l);
//
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		verify(tx).commit();
//	}
//
//	@Test
//	public void testTransactionRollback() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		try {
//			tt.execute(new TransactionCallback() {
//				@Override
//				public Object doInTransaction(TransactionStatus status) {
//					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//					SessionFactoryUtils.getSession(factory, true);
//					throw new RuntimeException("application exception");
//				}
//			});
//			fail("Should have thrown RuntimeException");
//		} catch (RuntimeException ex) {
//			// expected
//		}
//
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		verify(tx).rollback();
//	}
//
//	@Test
//	public void testTransactionRollbackWithAlreadyRolledBack() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		try {
//			tt.execute(new TransactionCallback() {
//				@Override
//				public Object doInTransaction(TransactionStatus status) {
//					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//					SessionFactoryUtils.getSession(factory, true);
//					throw new RuntimeException("application exception");
//				}
//			});
//			fail("Should have thrown RuntimeException");
//		} catch (RuntimeException ex) {
//			// expected
//		}
//
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//	}
//
//	@Test
//	public void testTransactionRollbackOnly() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//
//		tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
////				SessionFactoryUtils.getSession(factory, true).flush();
//				status.setRollbackOnly();
//				return null;
//			}
//		});
//
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//
//		verify(tx).rollback();
//	}
//
//	@Test
//	public void testParticipatingTransactionWithCommit() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		final TransactionTemplate tt = new TransactionTemplate(tm);
//		final List l = new ArrayList();
//		l.add("test");
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//
//				return tt.execute(new TransactionCallback() {
//					@Override
//					public Object doInTransaction(TransactionStatus status) {
////				SessionFactoryUtils.getSession(factory, true).flush();
//						return l;
//					}
//				});
//			}
//		});
//		assertTrue("Correct result list", result == l);
//	}
//
//	@Test
//	public void testParticipatingTransactionWithRollback() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		final TransactionTemplate tt = new TransactionTemplate(tm);
//		try {
//			tt.execute(new TransactionCallback() {
//				@Override
//				public Object doInTransaction(TransactionStatus status) {
//					return tt.execute(new TransactionCallback() {
//						@Override
//						public Object doInTransaction(TransactionStatus status) {
//							SessionFactoryUtils.getSession(factory, true);
//							throw new RuntimeException("application exception");
//						}
//					});
//				}
//			});
//			fail("Should have thrown RuntimeException");
//		} catch (RuntimeException ex) {
//			// expected
//		}
//		verify(tx).rollback();
//	}
//
//	@Test
//	public void testParticipatingTransactionWithRollbackOnly() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//		willThrow(new Exception()).given(tx).commit();
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		final TransactionTemplate tt = new TransactionTemplate(tm);
//		final List l = new ArrayList();
//		l.add("test");
//
//		try {
//			tt.execute(new TransactionCallback() {
//				@Override
//				public Object doInTransaction(TransactionStatus status) {
//					return tt.execute(new TransactionCallback() {
//						@Override
//						public Object doInTransaction(TransactionStatus status) {
////							SessionFactoryUtils.getSession(factory, true).flush();
//							status.setRollbackOnly();
//							return null;
//						}
//					});
//				}
//			});
//			fail("Should have thrown JdoResourceFailureException");
//		} catch (Exception ex) {
//			// expected
//		}
//	}
//
//	@Test
//	public void testParticipatingTransactionWithWithRequiresNew() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		final TransactionTemplate tt = new TransactionTemplate(tm);
//		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//		final List l = new ArrayList();
//		l.add("test");
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				return tt.execute(new TransactionCallback() {
//					@Override
//					public Object doInTransaction(TransactionStatus status) {
////						SessionFactoryUtils.getSession(factory, true).flush();
//						return l;
//					}
//				});
//			}
//		});
//		assertTrue("Correct result list", result == l);
//		verify(tx, times(2)).commit();
//	}
//
//	@Test
//	public void testParticipatingTransactionWithWithRequiresNewAndPrebound() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//		given(tx.status()).willReturn(Transaction.Status.OPEN);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		final TransactionTemplate tt = new TransactionTemplate(tm);
//		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//		final List l = new ArrayList();
//		l.add("test");
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//		TransactionSynchronizationManager.bindResource(factory, new SessionHolder(session));
//		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				SessionFactoryUtils.getSession(factory, true);
//
//				return tt.execute(new TransactionCallback() {
//					@Override
//					public Object doInTransaction(TransactionStatus status) {
////						SessionFactoryUtils.getSession(factory, true).flush();
//						return l;
//					}
//				});
//			}
//		});
//		assertTrue("Correct result list", result == l);
//
//		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//		TransactionSynchronizationManager.unbindResource(factory);
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		verify(tx, times(2)).commit();
//	}
//
//	@Test
//	public void testTransactionCommitWithPropagationSupports() {
//		given(factory.openSession()).willReturn(session);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
//		final List l = new ArrayList();
//		l.add("test");
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//				assertTrue("Is not new transaction", !status.isNewTransaction());
//				SessionFactoryUtils.getSession(factory, true);
//				return l;
//			}
//		});
//		assertTrue("Correct result list", result == l);
//
//		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(factory));
//	}
//
//	@Test
//	public void testIsolationLevel() {
//		given(factory.openSession()).willReturn(session);
//		given(session.beginTransaction()).willReturn(tx);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
//		tt.execute(new TransactionCallbackWithoutResult() {
//			@Override
//			protected void doInTransactionWithoutResult(TransactionStatus status) {
//			}
//		});
//	}
//
//	@Test
//	public void testTransactionCommitWithPrebound() {
//		given(session.beginTransaction()).willReturn(tx);
//
//		PlatformTransactionManager tm = new Neo4jTransactionManager(factory);
//		TransactionTemplate tt = new TransactionTemplate(tm);
//		final List l = new ArrayList();
//		l.add("test");
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//		TransactionSynchronizationManager.bindResource(factory, new SessionHolder(session));
//		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//
//		Object result = tt.execute(new TransactionCallback() {
//			@Override
//			public Object doInTransaction(TransactionStatus status) {
//				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//				SessionFactoryUtils.getSession(factory, true);
//				return l;
//			}
//		});
//		assertTrue("Correct result list", result == l);
//
//		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(factory));
//		TransactionSynchronizationManager.unbindResource(factory);
//		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
//
//		verify(tx).commit();
//	}
}
