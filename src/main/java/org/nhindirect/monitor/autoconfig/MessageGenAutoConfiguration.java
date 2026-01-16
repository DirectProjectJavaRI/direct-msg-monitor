package org.nhindirect.monitor.autoconfig;

import jakarta.annotation.PostConstruct;

import org.apache.camel.ProducerTemplate;
import org.nhindirect.common.mail.dsn.DSNGenerator;
import org.nhindirect.common.mail.dsn.impl.DefaultDSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.impl.HumanReadableTextAssemblerFactory;
import org.nhindirect.monitor.condition.impl.VariableCompletionCondition;
import org.nhindirect.monitor.processor.DSNMailSender;
import org.nhindirect.monitor.processor.DSNMessageGenerator;
import org.nhindirect.monitor.processor.SMTPClientFactory;
import org.nhindirect.monitor.processor.SMTPDSNMailSender;
import org.nhindirect.monitor.processor.StreamsDSNMailSender;
import org.nhindirect.monitor.processor.StreamsDSNMailSenderSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class MessageGenAutoConfiguration
{	
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
	
	@Value("${direct.msgmonitor.dsnGenerator.footer:<b><u>Troubleshooting Information</u></b><br/><br/>%headers_tag%}")	
	private String footer;
	
	@Value("${direct.msgmonitor.route.start.endpointuri:direct:start}")
	private String startURI;
	
	@Value("${direct.msgmonitor.dsnSender.gatewayURL:smtp://localhost:25}")
	private String gatewayURL;
	
	
	@ConditionalOnMissingBean
	@Bean
	DefaultDSNFailureTextBodyPartGenerator textBodyGenerator()
	{
		return new DefaultDSNFailureTextBodyPartGenerator(header, footer, failedRecipientsTitle, errorMessageTitle, defaultErrorMessage, 
				HumanReadableTextAssemblerFactory.getInstance());
	}
	
	@ConditionalOnMissingBean
	@Bean 
	DSNGenerator dsnGenerator()
	{
		return new DSNGenerator(subjectPrefix);
	}
	
	@ConditionalOnMissingBean
	@Bean 
	DSNMessageGenerator dsnMessageProcessor(VariableCompletionCondition variableCompletionCondition)
	{
		return new DSNMessageGenerator(dsnGenerator(), postmaster, variableCompletionCondition, mtaName, textBodyGenerator());
	}
	
	@PostConstruct
	public void setTemplateEndpoint()
	{		
		producerTemplate.setDefaultEndpointUri(startURI);
	}
	
	@ConditionalOnMissingBean
	@Bean 
	SMTPClientFactory smtpClientFactory()
	{
		return new SMTPClientFactory();
	}
	
	@Bean("dsnSender")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name="direct.msgmonitor.dsnSender.useStreamsSender", havingValue="true")
	DSNMailSender streamsDsnSender(Environment env, StreamBridge bridge)
	{
		final StreamsDSNMailSenderSource source = new StreamsDSNMailSenderSource(bridge);
		
		// make sure the SMTP Gateway is not enabled
		if (env.getProperty("direct.msgmonitor.dsnSender.useSMTPGatewaySender", "false").compareToIgnoreCase("true") == 0)
			throw new IllegalStateException("UseStreams and useSMTPGateway cannot both be set to true");
		
		final StreamsDSNMailSender retVal = new StreamsDSNMailSender(source);

		return retVal;

	}
	
	@Bean("dsnSender")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name="direct.msgmonitor.dsnSender.useSMTPGatewaySender", havingValue="true")
	DSNMailSender smtpDsnSender(Environment env)
	{
		// make sure the SMTP Gateway is not enabled
		if (env.getProperty("direct.msgmonitor.dsnSender.useStreamsSender", "false").compareToIgnoreCase("true") == 0)
			throw new IllegalStateException("UseStreams and useSMTPGateway cannot both be set to true");
		
		final SMTPDSNMailSender retVal = new SMTPDSNMailSender();
		retVal.setGatewayURL(gatewayURL);
		retVal.setSMTPClientFactory(smtpClientFactory());
		return retVal;

	}
	
}
