package org.nhindirect.monitor.distributedaggregatorroute;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.JPATestConfiguration;
import org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository;
import org.nhindirect.monitor.dao.impl.AddUpdateExceptionAggregationDAOImpl;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@RunWith(CamelSpringBootRunner.class)
@DataJpaTest
@Transactional
@ContextConfiguration(classes= {JPATestConfiguration.class, TestFailedAddUpdateExchangeMonitorRoute.AddUpdateConfiguration.class})
@DirtiesContext
public class TestFailedAddUpdateExchangeMonitorRoute extends CamelSpringTestSupport 
{
	@Configuration
	public static class AddUpdateConfiguration
	{
		@Bean
		public AddUpdateExceptionAggregationDAOImpl addUpdateExceptionAggregationDAOImpl()
		{
			return new AddUpdateExceptionAggregationDAOImpl();
		}
		
		@Bean
		public ConcurrentJPAAggregationRepository concurrentJPAAggregationRepository()
		{
			return new ConcurrentJPAAggregationRepository(addUpdateExceptionAggregationDAOImpl());
		}
	}
	

	
	@Autowired
	protected AddUpdateExceptionAggregationDAOImpl dao;
	
	@Override
	public void postProcessTest() throws Exception
	{
		super.postProcessTest();
		
		dao.purgeAll();
		
		assertEquals(0,dao.getAggregationKeys().size());
		assertEquals(0,dao.getAggregationCompletedKeys().size());
	}
	
	@SuppressWarnings("unchecked")
	@Test
    public void testSingleRecipMDNReceived_assertConditionComplete() throws Exception 
    {

		MockEndpoint mock = getMockEndpoint("mock:result");

		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com,ah4626@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		// send MDN to original messages
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		
		template.sendBody("direct:start", mdnMessage);
		
		mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "ah4626@direct.securehealthemail.com", 
				"gm2552@cerner.com", "ah4626@direct.securehealthemail.com");
		
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
		
		// validate the content of the exchange
		Collection<Tx> exBody = (Collection<Tx>)exchanges.iterator().next().getIn().getBody();
		assertEquals(3, exBody.size());
    }
	
	
	@Test
    public void testSingleRecipNoMDNReceived_assertConditionNotComplete() throws Exception 
    {

		MockEndpoint mock = getMockEndpoint("mock:result");


		// send original message
		final String originalMessageId = UUID.randomUUID().toString();

		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(0, exchanges.size());
    }
	
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() 
    {
    	return new ClassPathXmlApplicationContext("distributedAggregatorRoutes/monitor-route-to-mock-addupdate-error.xml");
    }
}
