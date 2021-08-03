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
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource( properties = "camel.springboot.xmlRoutes=classpath:routes/monitor-route-to-mock.xml")
public class TestNonReliableMessageMonitorRoute extends SpringBaseTest 
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

		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		// send MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	
	/*
	 * Keep around for testing with real messages
	@Test
    public void testSingleRecipMDNReceived_readFromMessages_assertConditionComplete() throws Exception 
    {
		final DefaultTxDetailParser parser = new DefaultTxDetailParser();
		
		final Map<String, TxDetail> imfDetails = parser.getMessageDetails(TestUtils.readMimeMessageFromFile("MessageFromCernerToAthena.txt")); 
		final Map<String, TxDetail> mdnDetails = parser.getMessageDetails(TestUtils.readMimeMessageFromFile("MDNFromAthenaToCerner")); 
		
		MockEndpoint mock = getMockEndpoint("mock:result");

		// send original message
		Tx originalMessage = new Tx(TxMessageType.IMF, imfDetails);
		template.sendBody("direct:start", originalMessage);

		// send MDN to original message
		Tx mdnMessage = new Tx(TxMessageType.MDN, mdnDetails);
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	*/
	
	@Test
    public void testSingleRecipMDNReceived_multipleMessage_assertSingleConditionComplete() throws Exception 
    {
		// send first message
		final String originalMessageId = UUID.randomUUID().toString();
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		// send second message
		Tx secondMessage = TestUtils.makeMessage(TxMessageType.IMF, UUID.randomUUID().toString(), "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", secondMessage);
		
		// send MDN to first message
		Tx mdnMessage =  TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	
	@Test
    public void testSingleRecipNoMDNReceived_assertConditionNotComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();

		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(0, exchanges.size());
    }
	
	@Test
    public void testSingleRecipFailedDSNReceived_assertConditionComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");	
		template.sendBody("direct:start", originalMessage);

		// send DSN to first message	
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.DSN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }	
	
	
	@Test
    public void testMulitipleRecips_SingleMDNReceived_assertConditionNotComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();

		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com," +
				"ah4626@direct.securehealthemail.com", "");			
		template.sendBody("direct:start", originalMessage);

		
		// send MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", "gm2552@cerner.com", 
				 "gm2552@direct.securehealthemail.com");			
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(0, exchanges.size());
    }
	
	@Test
    public void testMulitipleRecips_allMDNsReceived_assertConditionComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();
	
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com," +
				"ah4626@direct.securehealthemail.com", "");				
		template.sendBody("direct:start", originalMessage);

		
		// send MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", "gm2552@cerner.com", 
				 "gm2552@direct.securehealthemail.com");	
		template.sendBody("direct:start", mdnMessage);
		
		// send MDN to original message with the second recipient
		mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "ah4626@direct.securehealthemail.com", "gm2552@cerner.com", 
				"ah4626@direct.securehealthemail.com");	
		template.sendBody("direct:start", mdnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	
	@Test
    public void testMulitipleRecips_MDNReceivedandDSNReceived_assertConditionComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();
	
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com," +
				"ah4626@direct.securehealthemail.com", "");					
		template.sendBody("direct:start", originalMessage);

		
		// send MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", "gm2552@cerner.com", 
				"gm2552@direct.securehealthemail.com");			
		template.sendBody("direct:start", mdnMessage);
		
		// send DSN to original message with the second recipient
		Tx dsnMessage = TestUtils.makeMessage(TxMessageType.DSN, UUID.randomUUID().toString(), originalMessageId, "ah4626@direct.securehealthemail.com", "gm2552@cerner.com", 
				"ah4626@direct.securehealthemail.com");	
		template.sendBody("direct:start", dsnMessage);
				
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	
	@Test
    public void testMulitipleRecips_singleDSNReceivedWithAllRecipeints_assertConditionComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com," +
				"ah4626@direct.securehealthemail.com", "");		
		template.sendBody("direct:start", originalMessage);
		
		// send DSN to original message with the second recipient
		Tx dsnMessage = TestUtils.makeMessage(TxMessageType.DSN, UUID.randomUUID().toString(), originalMessageId, "ah4626@direct.securehealthemail.com", "gm2552@cerner.com", 
				"gm2552@direct.securehealthemail.com,ah4626@direct.securehealthemail.com");	
		template.sendBody("direct:start", dsnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(1, exchanges.size());
    }
	
	@Test
    public void testMulitipleRecips_singleDSNReceivedWithOneRecipeints_assertConditionNotComplete() throws Exception 
    {
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		Tx originalMessage =  TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com," +
				"ah4626@direct.securehealthemail.com", "");	
		
		template.sendBody("direct:start", originalMessage);
		
		// send DSN to original message with the second recipient
		Tx dsnMessage = TestUtils.makeMessage(TxMessageType.DSN, UUID.randomUUID().toString(), originalMessageId, "ah4626@direct.securehealthemail.com", "gm2552@cerner.com", 
				"gm2552@direct.securehealthemail.com");	
		template.sendBody("direct:start", dsnMessage);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		assertEquals(0, exchanges.size());
    }
}
