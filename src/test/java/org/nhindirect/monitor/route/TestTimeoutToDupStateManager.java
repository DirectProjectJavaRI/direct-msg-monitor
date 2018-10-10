package org.nhindirect.monitor.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:testTimeoutToDupStateManager.properties", properties = {"direct.msgmonitor.condition.generalConditionTimeout=1000", 
  "direct.msgmonitor.condition.reliableConditionTimeout=1000"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TestTimeoutToDupStateManager extends SpringBaseTest 
{
	@Autowired
	private ReceivedNotificationRepository recRepo;
	
	@Autowired
	protected CamelContext context;
	
	@EndpointInject(uri = "direct:start")
	protected ProducerTemplate template;
	
	public void purgeNotifDAO(ReceivedNotificationRepository recRepo) throws Exception
	{
		Calendar qualTime = Calendar.getInstance(Locale.getDefault());
		qualTime.add(Calendar.YEAR, 10);
		
		recRepo.deleteByReceivedTimeBefore(qualTime);
	}
	
	@Test
	public void testTimeoutReliableMessage_conditionNotComplete_assertDupAdded() throws Exception
	{
		assertNotNull(recRepo);
		purgeNotifDAO(recRepo);
		
		MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);

		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeReliableMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "", "", "");
		template.sendBody("direct:start", originalMessage);
		
		// no MDN sent... messages should timeout after 2 seconds
		// sleep 3 seconds to make sure it completes
		Thread.sleep(3000);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		/*
		 * One for the original message and one for the DNS message
		 */
		assertEquals(1, exchanges.size());
		Exchange exchange = exchanges.iterator().next();
		
		// make sure there is only 1 message in the exchange
		MimeMessage message = exchange.getIn().getBody(MimeMessage.class);
		assertNotNull(message);
		
		
		assertEquals("timeout", exchange.getProperty(Exchange.AGGREGATED_COMPLETED_BY));
		
		final String msgId = originalMessageId + "\t" + message.getMessageID();
		
		List<String> addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(msgId.toUpperCase(), 
				Arrays.asList("gm2552@direct.securehealthemail.com".toUpperCase()));
		assertEquals(1, addresses.size());
		assertTrue(addresses.contains("gm2552@direct.securehealthemail.com"));
		
		addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(originalMessageId.toUpperCase(), 
				Arrays.asList("gm2552@direct.securehealthemail.com".toUpperCase()));
		assertEquals(1, addresses.size());
		assertTrue(addresses.contains("gm2552@direct.securehealthemail.com"));		
	}	
	
	@Test
	public void testTimeoutReliableMessage_conditionNotComplete_msgNotReliable_assertDupNotAdded() throws Exception
	{
		assertNotNull(recRepo);
		purgeNotifDAO(recRepo);
		
		MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);

		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		template.sendBody("direct:start", originalMessage);

		// no MDN sent... messages should timeout after 2 seconds
		// sleep 3 seconds to make sure it completes
		Thread.sleep(3000);
		
		List<Exchange> exchanges = mock.getReceivedExchanges();
		
		/*
		 * One for the original message and one for the DNS message
		 */
		assertEquals(1, exchanges.size());
		Exchange exchange = exchanges.iterator().next();
		
		// make sure there is only 1 message in the exchange
		MimeMessage messages = exchange.getIn().getBody(MimeMessage.class);
		assertNotNull(messages);
		
		assertEquals("timeout", exchange.getProperty(Exchange.AGGREGATED_COMPLETED_BY));
		
		List<String> addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(originalMessageId.toUpperCase(), 
				Arrays.asList("gm2552@direct.securehealthemail.com".toUpperCase()));
		assertEquals(0, addresses.size());
	}	
	
	/*
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() 
    {
    	return new ClassPathXmlApplicationContext("routes/monitor-route-to-timeout-dupstate-manager.xml");
    }
    */
}
