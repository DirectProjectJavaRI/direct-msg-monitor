package org.nhindirect.monitor.processor;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.cloud.stream.function.StreamBridge;

public class StreamsDSNMailSenderSource 
{
	// Maps to the Spring Cloud Stream functional output binding name.
	protected static final String OUT_BINDING_NAME = "direct-smtp-gateway-message-out-0";
	
	private StreamBridge streamBridge;
	
	public StreamsDSNMailSenderSource(StreamBridge streamBridge)
	{
		this.streamBridge = streamBridge;
	}
	
	public <T> void sendMimeMessage(MimeMessage msg) throws MessagingException
	{

		streamBridge.send(OUT_BINDING_NAME, SMTPMailMessageConverter.toStreamMessage(mimeMsgToSMTPMailMessage(msg)));

	}

	public static SMTPMailMessage mimeMsgToSMTPMailMessage(MimeMessage msg) throws MessagingException
	{
		final InternetAddress sender = (msg.getFrom() != null && msg.getFrom().length > 0) ? (InternetAddress)msg.getFrom()[0] : null;
		final List<InternetAddress> recipients = new ArrayList<>(); 
		for (Address addr : msg.getAllRecipients())
			recipients.add((InternetAddress) addr);
		
		return new SMTPMailMessage(msg, recipients, sender);
	}
}
