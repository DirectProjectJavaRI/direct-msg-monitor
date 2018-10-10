package org.nhindirect.monitor.springconfig;

import javax.annotation.PostConstruct;

import org.apache.camel.ProducerTemplate;
import org.nhindirect.common.mail.dsn.DSNGenerator;
import org.nhindirect.common.mail.dsn.impl.DefaultDSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.impl.HumanReadableTextAssemblerFactory;
import org.nhindirect.monitor.condition.impl.VariableCompletionCondition;
import org.nhindirect.monitor.processor.DSNMailSender;
import org.nhindirect.monitor.processor.DSNMessageGenerator;
import org.nhindirect.monitor.processor.SMTPClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageGenConfig
{	
	@Autowired
	protected VariableCompletionCondition variableCompletionCondition;
	
	@Autowired
	protected ProducerTemplate producerTemplate;
	
	@Value("${direct.msgmonitor.dsnGenerator.postmasterName:postmaster}")	
	private String postmaster;
	
	@Value("${direct.msgmonitor.dsnGenerator.mtaName:DirectProject Message Monitor}")	
	private String mtaName;
	
	@Value("${direct.msgmonitor.dsnGenerator.subjectPrefix:Not Delivered:}")	
	private String subjectPrefix;
	
	@Value("${direct.msgmonitor.dsnGenerator.failedRecipientsTitle:We have not received a delivery notification in 1 hour for the following recipient(s) because the receiving system may be down or configured incorrectly:}")	
	private String failedRecipientsTitle;
	
	@Value("${direct.msgmonitor.dsnGenerator.errorMessageTitle:}")	
	private String errorMessageTitle;
	
	@Value("${direct.msgmonitor.dsnGenerator.defaultErrorMessage:<b>Your message most likely was not delivered.</b> Please confirm your recipient email addresses are correct. If the addresses are correct, consider a different communication method.<br/><br/>If you continue to receive this message, please have the recipient check with their system administrator and include the &ldquo;Troubleshooting Information&rdquo; below.}")	
	private String defaultErrorMessage;
	
	@Value("${direct.msgmonitor.dsnGenerator.header:%original_sender_tag%,<br/>}")	
	private String header;
	
	@Value("${direct.msgmonitor.dsnGenerator.footer:<u>Troubleshooting Information</u></b><br/><br/>%headers_tag%å}")	
	private String footer;
	
	@Value("${direct.msgmonitor.route.start.endpointuri:direct:start}")
	private String startURI;
	
	@Value("${direct.msgmonitor.dsnSender.gatewayURL:smtp://localhost:25}")
	private String gatewayURL;
	
	
	@Bean
	public DefaultDSNFailureTextBodyPartGenerator textBodyGenerator()
	{
		return new DefaultDSNFailureTextBodyPartGenerator(header, footer, failedRecipientsTitle, errorMessageTitle, defaultErrorMessage, 
				HumanReadableTextAssemblerFactory.getInstance());
	}
	
	@Bean 
	public DSNGenerator dsnGenerator()
	{
		return new DSNGenerator(subjectPrefix);
	}
	
	@Bean 
	public DSNMessageGenerator dsnMessageProcessor()
	{
		return new DSNMessageGenerator(dsnGenerator(), postmaster, variableCompletionCondition, mtaName, textBodyGenerator());
	}
	
	@PostConstruct
	public void setTemplateEndpoint()
	{		
		producerTemplate.setDefaultEndpointUri(startURI);
	}
	
	@Bean 
	public SMTPClientFactory smtpClientFactory()
	{
		return new SMTPClientFactory();
	}
	
	@Bean 
	public DSNMailSender dsnSender()
	{
		final DSNMailSender retVal = new DSNMailSender();
		retVal.setGatewayURL(gatewayURL);
		retVal.setSMTPClientFactory(smtpClientFactory());
		return retVal;

	}
	
}
