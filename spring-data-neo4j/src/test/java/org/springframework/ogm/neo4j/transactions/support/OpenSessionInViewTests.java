package org.springframework.ogm.neo4j.transactions.support;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactoryProvider;
import org.springframework.ogm.neo4j.support.OpenSessionInViewFilter;
import org.springframework.ogm.neo4j.support.OpenSessionInViewInterceptor;
import org.springframework.mock.web.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * Created by markangrish on 14/05/2016.
 */
public class OpenSessionInViewTests {

	private Session session;

	private SessionFactoryProvider sessionFactoryProvider;


	@Before
	public void setUp() throws Exception {
		sessionFactoryProvider = mock(SessionFactoryProvider.class);
		session = mock(Session.class);
		given(sessionFactoryProvider.openSession()).willReturn(session);

	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testOpenPersistenceManagerInViewInterceptor() throws Exception {

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactoryProvider(sessionFactoryProvider);

		MockHttpServletRequest request = new MockHttpServletRequest();

		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));

		assertNotNull(session);

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));

	}


	@Test
	public void testOpenPersistenceManagerInViewFilter() throws Exception {

		final SessionFactoryProvider sessionFactoryProvider2 = mock(SessionFactoryProvider.class);
		Session session2 = mock(Session.class);

		given(sessionFactoryProvider2.openSession()).willReturn(session2);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactoryProvider", sessionFactoryProvider);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactoryProvider", sessionFactoryProvider2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryProviderBeanName", "mySessionFactoryProvider");

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
					throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(sessionFactoryProvider2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactoryProvider2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactoryProvider));
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactoryProvider2));
		assertNotNull(request.getAttribute("invoked"));

		wac.close();
	}

}
