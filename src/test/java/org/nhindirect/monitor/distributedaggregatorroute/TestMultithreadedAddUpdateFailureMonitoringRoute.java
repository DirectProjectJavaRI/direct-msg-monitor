package org.nhindirect.monitor.distributedaggregatorroute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.nhindirect.monitor.util.TestUtils;

@TestPropertySource(properties = "camel.springboot.xmlRoutes=classpath:distributedAggregatorRoutes/multithreaded-route-to-mock.xml")
public class TestMultithreadedAddUpdateFailureMonitoringRoute extends SpringBaseTest 
{
	@Autowired
	protected CamelContext context;
	
	@Autowired
	private AggregationRepository aggRepo;
	
	@Autowired
	private AggregationCompletedRepository aggCompRepo;
	
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
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMultithreadedMDNResponseHandling_assertAllMDNsHandled() throws Exception
	{
		final StringBuilder recipBuilder = new StringBuilder(); 
		final Collection<String> recips = new ArrayList<String>();
		
		// create a list of 100 recipients
		for (int i = 0; i < 100; ++i)
		{
			final String recip = "recip" + (i + 1) + "@test.com";
			
			recips.add(recip);
			recipBuilder.append(recip);
			if (i != 99)
				recipBuilder.append(",");
		}

		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", recipBuilder.toString(), "");
		template.sendBody("seda:start", originalMessage);
		
		// now send the recipient MDN messages
		for (String recip : recips)
		{
			// send MDN to original messages
			Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, recip, 
					"gm2552@cerner.com",recip);
			
			template.sendBody("seda:start", mdnMessage);
		}
		
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
		
		// validate the content of the exchange
		Collection<Tx> exBody = (Collection<Tx>)exchanges.iterator().next().getIn().getBody();
		assertEquals(101, exBody.size());
	}
}
