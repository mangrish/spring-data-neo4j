package org.springframework.data.neo4j.transactions;

import static org.junit.Assert.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import org.junit.After;
import org.junit.Test;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Mark Angrish
 * @see DATAGRAPH-883
 */
public class TransactionEventsTest extends MultiDriverTestClass {

	private ConfigurableApplicationContext context;

	private EventCollector eventCollector;

	private TransactionTemplate transactionTemplate;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void immediately() {
		load(ImmediateTestListener.class);
		this.transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TransactionEventsTest.this.getContext().publishEvent("test");
				TransactionEventsTest.this.getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
				TransactionEventsTest.this.getEventCollector().assertTotalEventsCount(1);
				return null;
			}
		});
		getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	protected EventCollector getEventCollector() {
		return eventCollector;
	}

	protected ConfigurableApplicationContext getContext() {
		return context;
	}

	private void load(Class<?>... classes) {
		List<Class<?>> allClasses = new ArrayList<>();
		allClasses.add(BasicConfiguration.class);
		allClasses.addAll(Arrays.asList(classes));
		doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
	}

	private void doLoad(Class<?>... classes) {
		this.context = new AnnotationConfigApplicationContext(classes);
		this.eventCollector = this.context.getBean(EventCollector.class);
		this.transactionTemplate = new TransactionTemplate(this.context.getBean(PlatformTransactionManager.class));
	}

	@Configuration
	@ComponentScan({"org.springframework.data.neo4j.transactions"})
	@EnableTransactionManagement
	@EnableNeo4jRepositories
	static class BasicConfiguration extends Neo4jConfiguration {

		@Bean // set automatically with tx management
		public TransactionalEventListenerFactory transactionalEventListenerFactory() {
			return new TransactionalEventListenerFactory();
		}

		@Bean
		public EventCollector eventCollector() {
			return new EventCollector();
		}

		@Bean
		public SessionFactory getSessionFactory() {
			return new SessionFactory("org.springframework.data.neo4j.transactions");
		}

//		@Bean
//		public PlatformTransactionManager transactionManager() throws Exception {
//			SessionFactory sessionFactory = getSessionFactory();
//			return new TransactionEventsTest.CallCountingTransactionManager(sessionFactory);
//		}
	}


	static class EventCollector {

		public static final String IMMEDIATELY = "IMMEDIATELY";

		public static final String BEFORE_COMMIT = "BEFORE_COMMIT";

		public static final String AFTER_COMPLETION = "AFTER_COMPLETION";

		public static final String AFTER_COMMIT = "AFTER_COMMIT";

		public static final String AFTER_ROLLBACK = "AFTER_ROLLBACK";

		public static final String[] ALL_PHASES = {IMMEDIATELY, BEFORE_COMMIT, AFTER_COMMIT, AFTER_ROLLBACK};

		private final MultiValueMap<String, Object> events = new LinkedMultiValueMap<>();

		public void addEvent(String phase, Object event) {
			this.events.add(phase, event);
		}

		public List<Object> getEvents(String phase) {
			List<Object> v;
			return (((v = events.get(phase)) != null) || events.containsKey(phase))
					? v
					: Collections.emptyList();
		}

		public void assertNoEventReceived(String... phases) {
			if (phases.length == 0) { // All values if none set
				phases = ALL_PHASES;
			}
			for (String phase : phases) {
				List<Object> eventsForPhase = getEvents(phase);
				assertEquals("Expected no events for phase '" + phase + "' " +
						"but got " + eventsForPhase + ":", 0, eventsForPhase.size());
			}
		}

		public void assertEvents(String phase, Object... expected) {
			List<Object> actual = getEvents(phase);
			assertEquals("wrong number of events for phase '" + phase + "'", expected.length, actual.size());
			for (int i = 0; i < expected.length; i++) {
				assertEquals("Wrong event for phase '" + phase + "' at index " + i, expected[i], actual.get(i));
			}
		}

		public void assertTotalEventsCount(int number) {
			int size = 0;
			for (Map.Entry<String, List<Object>> entry : this.events.entrySet()) {
				size += entry.getValue().size();
			}
			assertEquals("Wrong number of total events (" + this.events.size() + ") " +
					"registered phase(s)", number, size);
		}
	}


	static abstract class BaseTransactionalTestListener {

		static final String FAIL_MSG = "FAIL";

		@Autowired
		private EventCollector eventCollector;

		public void handleEvent(String phase, String data) {
			this.eventCollector.addEvent(phase, data);
			if (FAIL_MSG.equals(data)) {
				throw new IllegalStateException("Test exception on phase '" + phase + "'");
			}
		}
	}


	@Component
	static class ImmediateTestListener extends BaseTransactionalTestListener {

		@EventListener(condition = "!'SKIP'.equals(#data)")
		public void handleImmediately(String data) {
			handleEvent(EventCollector.IMMEDIATELY, data);
		}
	}


	@Component
	static class AfterCompletionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMPLETION)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}


	@Component
	static class AfterCompletionExplicitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMMIT)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}
	}


	@Transactional
	@Component
	static interface TransactionalComponentTestListenerInterface {

		// Cannot use #data in condition due to dynamic proxy.
		@TransactionalEventListener(condition = "!'SKIP'.equals(#p0)")
		void handleAfterCommit(String data);
	}


	static class TransactionalComponentTestListener extends BaseTransactionalTestListener implements
			TransactionalComponentTestListenerInterface {

		@Override
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}


	@Component
	static class BeforeCommitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT)
		@Order(15)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}
	}


	@Component
	static class FallbackExecutionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK, fallbackExecution = true)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}

		@TransactionalEventListener(phase = AFTER_COMPLETION, fallbackExecution = true)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}


	@TransactionalEventListener(phase = AFTER_COMMIT, condition = "!'SKIP'.equals(#p0)")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface AfterCommitEventListener {

	}


	@Component
	static class AfterCommitMetaAnnotationTestListener extends BaseTransactionalTestListener {

		@AfterCommitEventListener
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}


	static class EventTransactionSynchronization extends TransactionSynchronizationAdapter {

		private final int order;

		EventTransactionSynchronization(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

//	public static class CallCountingTransactionManager extends Neo4jTransactionManager {
//
//		public TransactionDefinition lastDefinition;
//		public int begun;
//		public int commits;
//		public int rollbacks;
//		public int inflight;
//
//		public CallCountingTransactionManager(SessionFactory session) {
//			super(session);
//		}
//
//		@Override
//		protected Object doGetTransaction() {
//			return new Object();
//		}
//
//		@Override
//		protected void doBegin(Object transaction, TransactionDefinition definition) {
//			this.lastDefinition = definition;
//			++begun;
//			++inflight;
//
//		}
//
//		@Override
//		protected void doCommit(DefaultTransactionStatus status) {
//			++commits;
//			--inflight;
//		}
//
//		@Override
//		protected void doRollback(DefaultTransactionStatus status) {
//			++rollbacks;
//			--inflight;
//		}
//
//		public void clear() {
//			begun = commits = rollbacks = inflight = 0;
//		}
//	}
}
