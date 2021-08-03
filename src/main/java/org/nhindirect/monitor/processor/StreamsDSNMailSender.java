package org.nhindirect.monitor.processor;

import javax.mail.internet.MimeMessage;

import org.apache.camel.Exchange;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamsDSNMailSender implements DSNMailSender
{
	protected StreamsDSNMailSenderSource streamSource;
	
	public StreamsDSNMailSender(StreamsDSNMailSenderSource streamSource)
	{
		this.streamSource = streamSource;
	}

	@Override
	public void sendMail(Exchange exchange) throws Exception 
	{
    	if (streamSource == null)
    		throw new IllegalStateException("Stream source is null");
    	
    	if (exchange.getIn() == null || exchange.getIn().getBody() == null)
    		return;
    	
    	final MimeMessage dsnMessage = (MimeMessage)exchange.getIn().getBody();
		
    	try
    	{
    		streamSource.sendMimeMessage(dsnMessage);
    	}
    	catch (Exception e)
    	{
    		log.error("Error sending message on stream source");
    		throw e;
    	}
	}
}
