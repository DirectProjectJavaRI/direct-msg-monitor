package org.nhindirect.monitor.processor;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.mail.dsn.DSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.DSNGenerator;
import org.nhindirect.common.mail.dsn.impl.DefaultDSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.impl.HumanReadableTextAssemblerFactory;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.condition.TxCompletionCondition;
import org.nhindirect.monitor.condition.impl.GeneralCompletionCondition;
import org.nhindirect.monitor.util.TestUtils;

public class DSNMessageGenerator_generateDSNFailureMessageTest 
{
	DSNMessageGenerator createGenerator()
	{
		DSNGenerator dsnGenerator = new DSNGenerator("Not Delivered:");

		TxCompletionCondition checker = new GeneralCompletionCondition();
		DSNFailureTextBodyPartGenerator textGenerator = new DefaultDSNFailureTextBodyPartGenerator("", "", "",
			    "", "", HumanReadableTextAssemblerFactory.getInstance());
		

		return new DSNMessageGenerator(dsnGenerator, "postmaster", checker, "JUnitMTA", textGenerator);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGenerateDSNFailureMessage_assertMessageCreated() throws Exception
	{
		
		Exchange exchange = new DefaultExchange(mock(CamelContext.class));

		
		String message = TestUtils.readMessageFromFile("MessageWithAttachment.txt");
		DefaultTxDetailParser parser = new DefaultTxDetailParser();
		InputStream str = IOUtils.toInputStream(message);
		
		try
		{
			// send original message
			final MimeMessage mimeMessage = new MimeMessage(null, str);
			
			// change the message id
			mimeMessage.saveChanges();
			
			Map<String, TxDetail> details = parser.getMessageDetails(mimeMessage);
			Tx originalMessage = new Tx(TxMessageType.IMF, details);
			Collection<Tx> txs = new ArrayList<Tx>();
			txs.add(originalMessage);
			
			DSNMessageGenerator generator = createGenerator();
		    generator.generateDSNFailureMessage(txs, exchange);
		    
		    MimeMessage dsnMessage = (MimeMessage)exchange.getIn().getBody();
		    assertNotNull(dsnMessage);
		    
		    // check the subject
		    String newSubject = MailStandard.getHeader(dsnMessage, MailStandard.Headers.Subject);
		    assertTrue(newSubject.contains(MailStandard.getHeader(mimeMessage, MailStandard.Headers.Subject)));
			
		    // check for the original message id in the in-reply-to
		    String originalMessageId = MailStandard.getHeader(dsnMessage, MailStandard.Headers.InReplyTo);
		    assertEquals(MailStandard.getHeader(mimeMessage, MailStandard.Headers.MessageID), originalMessageId);
		    
		}
		finally
		{
			IOUtils.closeQuietly(str);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGenerateDSNFailureMessage_assertMessageCreated_noCopiedSubject() throws Exception
	{
		
		Exchange exchange = new DefaultExchange(mock(CamelContext.class));

		
		String message = TestUtils.readMessageFromFile("MessageWithAttachment.txt");
		DefaultTxDetailParser parser = new DefaultTxDetailParser();
		InputStream str = IOUtils.toInputStream(message);
		
		try
		{
			// send original message
			final MimeMessage mimeMessage = new MimeMessage(null, str);
			
			mimeMessage.setHeader(MailStandard.Headers.Subject, "");
			
			// change the message id
			mimeMessage.saveChanges();
			
			Map<String, TxDetail> details = parser.getMessageDetails(mimeMessage);
			Tx originalMessage = new Tx(TxMessageType.IMF, details);
			Collection<Tx> txs = new ArrayList<Tx>();
			txs.add(originalMessage);
			
			DSNMessageGenerator generator = createGenerator();
		    generator.generateDSNFailureMessage(txs, exchange);
		    
		    MimeMessage dsnMessage = (MimeMessage)exchange.getIn().getBody();
		    assertNotNull(dsnMessage);
		    
		    // check the subject
		    String newSubject = MailStandard.getHeader(dsnMessage, MailStandard.Headers.Subject);
		    assertEquals("Not Delivered:", newSubject);
			
		}
		finally
		{
			IOUtils.closeQuietly(str);
		}
	}
	
	@Test
	public void testGenerateDSNFailureMessage_noFrom_assertException() throws Exception
	{
		
		Exchange exchange = new DefaultExchange(mock(CamelContext.class));
	
		
		// original message
		final String originalMessageId = UUID.randomUUID().toString();	
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "", "gm2552@direct.securehealthemail.com", "");
		
		Collection<Tx> txs = new ArrayList<Tx>();
		txs.add(originalMessage);
		
		DSNMessageGenerator generator = createGenerator();
		
		boolean exceptionOccurred = false;
		
		try
		{	
		    generator.generateDSNFailureMessage(txs, exchange);
		}
		catch (MessagingException e)
		{
			exceptionOccurred = true;	
		}
		
		assertTrue(exceptionOccurred);

	}
	
	@Test
	public void testGenerateDSNFailureMessage_nullMessageToTrack() throws Exception
	{
		
		Exchange exchange = new DefaultExchange(mock(CamelContext.class));
	
		Collection<Tx> txs = new ArrayList<Tx>();
		
		DSNMessageGenerator generator = createGenerator();
	    generator.generateDSNFailureMessage(txs, exchange);
	    
	    MimeMessage dsnMessage = (MimeMessage)exchange.getIn().getBody();
	    assertNull(dsnMessage);
		   
	}
	
	@Test
	public void testGenerateDSNFailureMessage_noIncompleteRecipients() throws Exception
	{
		
		Exchange exchange = new DefaultExchange(mock(CamelContext.class));
	
		
		// original message
		final String originalMessageId = UUID.randomUUID().toString();	
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");

		// MDN to original message
		Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
				"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
		
		Collection<Tx> txs = new ArrayList<Tx>();
		txs.add(originalMessage);
		txs.add(mdnMessage);
		
		DSNMessageGenerator generator = createGenerator();
	    generator.generateDSNFailureMessage(txs, exchange);
	    
	    MimeMessage dsnMessage = (MimeMessage)exchange.getIn().getBody();
	    assertNull(dsnMessage);
		   
	}
	
}
