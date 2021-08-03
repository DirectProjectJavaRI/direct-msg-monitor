package org.nhindirect.monitor.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.nhindirect.monitor.resources.TxsResource;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "camel.springboot.xmlRoutes=classpath:routes/monitor-route-to-mock-with-configured-template.xml")
public class TestResourceMonitorRoute extends SpringBaseTest 
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
	
	@Test
    public void testSingleRecipMDNReceived_assertConditionComplete() throws Exception 
    {

		template.setDefaultEndpointUri("direct:start");
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		TxsResource resource = new TxsResource(template, null);
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		resource.addTx(originalMessage);

		// send MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		resource.addTx(mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
}
