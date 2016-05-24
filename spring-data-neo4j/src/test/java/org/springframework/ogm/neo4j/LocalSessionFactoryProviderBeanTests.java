package org.springframework.ogm.neo4j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.exception.MappingException;
import org.neo4j.ogm.exception.UnknownStatementTypeException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.examples.friends.FriendService;
import org.springframework.data.neo4j.examples.friends.context.FriendContext;
import org.springframework.data.neo4j.examples.friends.repo.FriendshipRepository;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Created by markangrish on 24/05/2016.
 */
public class LocalSessionFactoryProviderBeanTests extends MultiDriverTestClass {


	protected static SessionFactoryProvider mockSfp;


	@Before
	public void setUp() throws Exception {
		mockSfp = mock(SessionFactoryProvider.class);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	@Test
	public void testExceptionTranslationWithNoDialect() throws Exception {
		LocalSessionFactoryProviderBean lsfb = new LocalSessionFactoryProviderBean();
		lsfb.setPackagesToScan("org.springframework.data.neo4j.examples.movies.domain");
		lsfb.afterPropertiesSet();
		lsfb.getObject();

		RuntimeException in1 = new RuntimeException("UnknownStatementTypeException");
		UnknownStatementTypeException in2 = new UnknownStatementTypeException("asdasd");
		assertNull("No translation here", lsfb.translateExceptionIfPossible(in1));
		DataAccessException dex = lsfb.translateExceptionIfPossible(in2);
		assertNotNull(dex);
		assertSame(in2, dex.getCause());
	}
}
