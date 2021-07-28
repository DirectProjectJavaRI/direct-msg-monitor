package org.nhindirect.monitor.processor;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


import java.io.File;
import java.util.Properties;

import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.monitor.TestApplication;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

public class StreamsDSNMailSender_sendMailTest
{
	@Test
	public void testSendMail_mailSent_messageSentAndReceived() throws Exception
	{
		final Properties props = new Properties();
		props.load(FileUtils.openInputStream(new File("./src/test/resources/bootstrap.properties")));
		props.setProperty("direct.msgmonitor.dsnSender.useStreamsSender", "true");
		
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(
						TestApplication.class)).properties(props)
				.run("")) 
		{
			final MimeMessage msg = TestUtils.readMimeMessageFromFile("MessageWithAttachment.txt");
			final OutputDestination output = context.getBean(OutputDestination.class);
			final DSNMailSender sender = context.getBean(DSNMailSender.class);
			
			Exchange exchange = new DefaultExchange(mock(CamelContext.class));
			exchange.getIn().setBody(msg);
			
			sender.sendMail(exchange);
		
			
	        final Message<?> streamMsg  = output.receive();
	        
	        assertNotNull(streamMsg);
	        
	        final SMTPMailMessage smtpMailMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
	        
	        assertNotNull(smtpMailMessage);
			
	        assertEquals(msg.getFrom()[0], smtpMailMessage.getMailFrom());
		};
	}
}
