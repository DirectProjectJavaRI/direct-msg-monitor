package org.nhindirect.monitor.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.PendingNotificationRepository;
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
	static {
		System.setProperty(
			    "mail.mime.ignoremissingcontenthandler",
			    "true"
			);

	}
	
	@Autowired
	private ReceivedNotificationRepository recRepo;

	@Autowired
	private PendingNotificationRepository pendingRepo;

	@Autowired
	protected CamelContext context;
	
	@EndpointInject("direct:start")
	protected ProducerTemplate template;
	
	public void purgeNotifDAO(ReceivedNotificationRepository recRepo) throws Exception
	{
		Calendar qualTime = Calendar.getInstance(Locale.getDefault());
		qualTime.add(Calendar.YEAR, 10);
		
		recRepo.deleteByReceivedTimeBefore(qualTime);
	}
	
	/**
	 * Sends a reliable message with no MDN response, lets the completion condition time out, then drives the
	 * generated DSN through {@code /txs/suppressNotification} (the same call the gateway makes) to promote its
	 * pending entries to the duplicate store.  Asserts the full generate -&gt; pending -&gt; promote lifecycle
	 * along the way.
	 * @return The message id of the generated DSN.
	 */
	private String timeoutAndPromoteGeneratedDsn(String originalMessageId, String recipient) throws Exception
	{
		MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);

		Tx originalMessage = TestUtils.makeReliableMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", recipient, "", "", "");
		template.sendBody("direct:start", originalMessage);

		// no MDN sent... messages should timeout after 2 seconds
		// sleep 3 seconds to make sure it completes
		Thread.sleep(3000);

		List<Exchange> exchanges = mock.getReceivedExchanges();

		/*
		 * One for the generated DSN message
		 */
		assertEquals(1, exchanges.size());
		Exchange exchange = exchanges.iterator().next();

		// make sure there is only 1 message in the exchange
		MimeMessage message = exchange.getIn().getBody(MimeMessage.class);
		assertNotNull(message);

		assertEquals("timeout", exchange.getProperty(Exchange.AGGREGATED_COMPLETED_BY));

		final String dsnMsgId = message.getMessageID();

		// the generated DSN has not made it back through the gateway yet, so it must not be in the
		// duplicate store yet...
		List<String> addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(originalMessageId.toUpperCase(),
				Arrays.asList(recipient.toUpperCase()));
		assertEquals(0, addresses.size());

		// ...and must be recorded as pending instead
		addresses = pendingRepo.findByDsnMessageIdIgnoreCaseAndAddressInIgnoreCase(dsnMsgId.toUpperCase(),
				Arrays.asList(recipient.toUpperCase()));
		assertEquals(1, addresses.size());
		assertTrue(addresses.contains(recipient));

		// simulate the gateway's suppression check on the generated DSN as it comes back through tracking.
		// This is the point at which the pending entry is promoted to the duplicate store.
		final Tx generatedDsn = TestUtils.makeMessage(TxMessageType.DSN, dsnMsgId, originalMessageId, "", "", recipient);

		final Boolean suppressed = checkSuppressNotification(generatedDsn);

		// the monitor's own generated DSN must never be suppressed on this first pass, or the gateway would
		// never deliver it to the sender
		assertFalse(suppressed);

		// the pending entry has now been promoted to the duplicate store...
		addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(originalMessageId.toUpperCase(),
				Arrays.asList(recipient.toUpperCase()));
		assertEquals(1, addresses.size());
		assertTrue(addresses.contains(recipient));

		// ...and removed from the pending store
		addresses = pendingRepo.findByDsnMessageIdIgnoreCaseAndAddressInIgnoreCase(dsnMsgId.toUpperCase(),
				Arrays.asList(recipient.toUpperCase()));
		assertEquals(0, addresses.size());

		return dsnMsgId;
	}

	private Boolean checkSuppressNotification(Tx tx)
	{
		return webClient.post().uri("/txs/suppressNotification")
				.bodyValue(tx).retrieve()
				.bodyToMono(Boolean.class).block();
	}

	@Test
	public void testTimeoutReliableMessage_conditionNotComplete_assertPendingAddedThenPromotedOnSuppressCheck() throws Exception
	{
		assertNotNull(recRepo);
		assertNotNull(pendingRepo);
		purgeNotifDAO(recRepo);
		pendingRepo.deleteAll();

		final String originalMessageId = UUID.randomUUID().toString();
		final String recipient = "gm2552@direct.securehealthemail.com";

		timeoutAndPromoteGeneratedDsn(originalMessageId, recipient);
	}

	@Test
	public void testTimeoutReliableMessage_duplicateDeliveryOfGeneratedDsn_assertSuppressed() throws Exception
	{
		assertNotNull(recRepo);
		assertNotNull(pendingRepo);
		purgeNotifDAO(recRepo);
		pendingRepo.deleteAll();

		final String originalMessageId = UUID.randomUUID().toString();
		final String recipient = "gm2552@direct.securehealthemail.com";

		final String dsnMsgId = timeoutAndPromoteGeneratedDsn(originalMessageId, recipient);

		// The gateway (or the underlying broker) redelivers the exact same generated DSN a second time.  Its
		// pending entry is already gone (it was promoted on the first pass above), so this must now fall
		// through to the normal suppression check, which finds the promoted duplicate store entry and
		// suppresses the redelivered DSN.
		final Tx redeliveredDsn = TestUtils.makeMessage(TxMessageType.DSN, dsnMsgId, originalMessageId, "", "", recipient);

		final Boolean suppressed = checkSuppressNotification(redeliveredDsn);
		assertTrue(suppressed);
	}

	@Test
	public void testTimeoutReliableMessage_laterMdnForSameRecipient_assertSuppressed() throws Exception
	{
		assertNotNull(recRepo);
		assertNotNull(pendingRepo);
		purgeNotifDAO(recRepo);
		pendingRepo.deleteAll();

		final String originalMessageId = UUID.randomUUID().toString();
		final String recipient = "gm2552@direct.securehealthemail.com";

		timeoutAndPromoteGeneratedDsn(originalMessageId, recipient);

		// A genuine MDN (dispatched/processed) later arrives from the recipient's system for the same original
		// message and recipient.  The monitor already closed this recipient's state when the generated DSN was
		// promoted to the duplicate store above, so this later MDN must be suppressed.
		final Tx lateMdn = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "", "", recipient,
				"", MDNStandard.Disposition_Processed);

		final Boolean suppressed = checkSuppressNotification(lateMdn);
		assertTrue(suppressed);
	}
	
	@Test
	public void testTimeoutReliableMessage_conditionNotComplete_msgNotReliable_assertDupNotAdded() throws Exception
	{
		assertNotNull(recRepo);
		purgeNotifDAO(recRepo);
		
		MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
		mock.setLog(true);
		
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
	
}
