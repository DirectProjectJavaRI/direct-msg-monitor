package org.nhindirect.monitor.distributedaggregatorroute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.RecoverableAggregationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.nhindirect.monitor.util.TestUtils;

@TestPropertySource(locations="classpath:properties/recoveryMonitor.properties", 
properties = "camel.springboot.xmlRoutes=classpath:distributedAggregatorRoutes/recover-exchange-to-mock.xml")
public class TestRecoveryMonitorRoute extends SpringBaseTest 
{
	@Autowired
	protected CamelContext context;
	
	@Autowired
	private AggregationRepository aggRepo;
	
	@Autowired
	private AggregationCompletedRepository aggCompRepo;
	
	@Autowired
	private RecoverableAggregationRepository repo;
	
	protected MockEndpoint mock;
	
	protected ProducerTemplate template;
	
	@BeforeEach
	public void setUp()
	{
		super.setUp();
		
		aggRepo.deleteAll();
		aggCompRepo.deleteAll();
		
		mock = (MockEndpoint)context.getEndpoint("mock:result");
		mock.reset();
		
		template = context.createProducerTemplate();
		
		// pre populate some recovery data
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		final Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com,ah4626@direct.securehealthemail.com", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(originalMessage);

		repo.add(context, originalMessageId, exchange);
		
		repo.remove(context, originalMessageId, exchange);
		
		// lock the row to create a delay and ensure we recover 
		// exchange ids that return null at some point
		repo.recover(context, exchange.getExchangeId());		
	}	
	
	@SuppressWarnings("deprecation")
	@Test
	public void testRecoverFromRepository() throws Exception
	{		
		boolean exchangeFound = false;
		int cnt = 0;
		
		List<Exchange> exchanges = null;
		while (cnt < 10)
		{
			exchanges = mock.getReceivedExchanges();
			if (exchanges.size() == 1)
			{
				exchangeFound = true;
				break;
			}
			
			++cnt;
			Thread.sleep(2000);
		}
		
		assertTrue(exchangeFound);
		Tx originalMessage = (Tx)exchanges.iterator().next().getIn().getBody();
		assertEquals("gm2552@cerner.com", originalMessage.getDetail(TxDetailType.FROM).getDetailValue());
		
		// make sure everything got confirmed
		final AggregationRepository aggRepo = context.getRegistry().lookupByType(AggregationRepository.class).values().iterator().next();
		final AggregationCompletedRepository aggCompRepo = context.getRegistry().lookupByType(AggregationCompletedRepository.class).values().iterator().next();
		
		
		assertEquals(0,aggRepo.findAllKeys().size());
		assertEquals(0,aggCompRepo.findAllKeys().size());
		
	}
}
