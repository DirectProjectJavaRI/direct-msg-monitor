package org.nhindirect.monitor;

import javax.annotation.PostConstruct;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.nhindirect.common.mail.dsn.DSNGenerator;
import org.nhindirect.common.mail.dsn.impl.DefaultDSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.impl.HumanReadableTextAssemblerFactory;
import org.nhindirect.monitor.aggregator.BasicTxAggregator;
import org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository;
import org.nhindirect.monitor.condition.impl.DecayingTimeoutCondition;
import org.nhindirect.monitor.condition.impl.GeneralCompletionCondition;
import org.nhindirect.monitor.condition.impl.TimelyAndReliableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableTimeoutCondition;
import org.nhindirect.monitor.dao.AggregationDAO;
import org.nhindirect.monitor.dao.NotificationDuplicationDAO;
import org.nhindirect.monitor.expression.MessageIdCorrelationExpression;
import org.nhindirect.monitor.processor.DSNMessageGenerator;
import org.nhindirect.monitor.processor.impl.TimeoutDupStateManager;
import org.nhindirect.monitor.resources.TxsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.nhindirect.monitor"})	
public class TestApplication
{
	
	@Value("${monitor.condition.generalConditionTimeout:3600000}")
	protected long generalTimeout;
	
	@Value("${monitor.condition.reliableConditionTimeout:3600000}")
	protected long reliableTimeout;
	
	@Value("${monitor.recovery.retryInterval:300000}")
	protected long recoveryInterval;
	
	@Value("${monitor.recovery.maxRetryAttemps:12}")
	protected int maximumRedeliveries;
	
	@Value("${monitor.recovery.deadLetterURL:file:recovery/directMonitorDeadLetter}")
	protected String deadLetterUri;	

	@Autowired
	protected NotificationDuplicationDAO dao;
	
	@Qualifier("aggregationDAOImpl")
	@Autowired
	protected AggregationDAO aggDao;
	
	@EndpointInject(uri = "direct:start")
	protected ProducerTemplate producerTemplate;
	
	@Autowired
	protected TxsResource txResource;
	
    public static void main(String[] args) 
    {
        SpringApplication.run(TestApplication.class, args);
    }  
    
    @Bean
    public BasicTxAggregator aggregationStrategy()
    {
    	return new BasicTxAggregator(variableCompletionCondition(), variableTimeoutCondition());
    }
    
    @Bean 
    public VariableCompletionCondition variableCompletionCondition()
    {
    	return new VariableCompletionCondition(timelyAndReliableCompletionCondition(), generalCompletionCondition());
    }
    
    @Bean
    public TimelyAndReliableCompletionCondition timelyAndReliableCompletionCondition()
    {
    	final TimelyAndReliableCompletionCondition retVal = new TimelyAndReliableCompletionCondition();
    	retVal.setDupDAO(dao);
    	
    	return retVal;
    }
    
    @Bean
    public GeneralCompletionCondition generalCompletionCondition()
    {
    	return new GeneralCompletionCondition();
    }
    
    @Bean 
    public VariableTimeoutCondition variableTimeoutCondition()
    {
    	return new VariableTimeoutCondition(generalTimeoutCondition(), reliableTimeoutCondition());
    }
    
    @Bean
    public DecayingTimeoutCondition generalTimeoutCondition()
    {
    	return new DecayingTimeoutCondition(generalTimeout);
    }
    
    @Bean
    public DecayingTimeoutCondition reliableTimeoutCondition()
    {
    	return new DecayingTimeoutCondition(reliableTimeout);
    }    
    
    @Bean
    public ConcurrentJPAAggregationRepository directMonitoringRepo()
    {
    	final ConcurrentJPAAggregationRepository retVal = new ConcurrentJPAAggregationRepository();
    	retVal.setAggreationDAO(aggDao);
    	retVal.setRecoveryInterval(recoveryInterval);
    	retVal.setMaximumRedeliveries(maximumRedeliveries);
    	retVal.setDeadLetterUri(deadLetterUri);
    	
    	return retVal;
    }
    
	@Bean 
	public MessageIdCorrelationExpression msgIdCorrelator()
	{
		return new MessageIdCorrelationExpression();
	}    

	@Bean
	public HumanReadableTextAssemblerFactory textAssemblerFactor()
	{
		return new HumanReadableTextAssemblerFactory();
	}
	
	@Bean
	public DefaultDSNFailureTextBodyPartGenerator textBodyGenerator()
	{
		return new DefaultDSNFailureTextBodyPartGenerator("%original_sender_tag%,&lt;br/&gt;", "The &lt;i&gt;Cerner Direct&lt;/i&gt; Team&lt;br/&gt;&lt;br/&gt;&lt;b&gt;&lt;u&gt;Troubleshooting Information&lt;/u&gt;&lt;/b&gt;&lt;br/&gt;&lt;br/&gt;%headers_tag%", 
				"We have not received a delivery notification for the following recipient(s) because the receiving system may be down or configured incorrectly:", "", 
				"&lt;b&gt;Your message delivery has been delayed.&lt;/b&gt; Please confirm your recipient email addresses are correct. If the addresses are correct and the message is time sensitive, consider a different communication method. We will send another notification to you in 24 hours if we still have not received a delivery notification.&lt;br&gt;&lt;br/&gt;If you continue to receive this message, please have the recipient check with their system admin and include the &quot;Troubleshooting Information&quot; below.", 
				textAssemblerFactor());
	}


	@Bean
	public DSNGenerator dsnGenerator()
	{
		return new DSNGenerator("Not Delivered:");
	}
	
	@Bean
	public DSNMessageGenerator dsnMessageProcessor()
	{
		return new DSNMessageGenerator(dsnGenerator(), "postmaster", variableCompletionCondition(), "DirectMessageMonitor", textBodyGenerator());
	}

	@Bean 
	public TimeoutDupStateManager duplicationStateManager()
	{
		final TimeoutDupStateManager retVal = new TimeoutDupStateManager();
		retVal.setDao(dao);
		retVal.setMessageRetention(8);

		return retVal;
	}
	
	@PostConstruct
	public void setTemplateEndpoint()
	{
		txResource.setProducerTemplate(producerTemplate);
	} 
}
