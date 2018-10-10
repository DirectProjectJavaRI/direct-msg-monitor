package org.nhindirect.monitor.springconfig;

import org.nhindirect.monitor.aggregator.BasicTxAggregator;
import org.nhindirect.monitor.condition.impl.DecayingTimeoutCondition;
import org.nhindirect.monitor.condition.impl.GeneralCompletionCondition;
import org.nhindirect.monitor.condition.impl.TimelyAndReliableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableTimeoutCondition;
import org.nhindirect.monitor.expression.MessageIdCorrelationExpression;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConditionConfig
{
	@Autowired
	protected ReceivedNotificationRepository recRepo;
	
	@Value("${direct.msgmonitor.condition.generalConditionTimeout:3600000}")	
	private String generalConditionTimeout;
	
	@Value("${direct.msgmonitor.condition.reliableConditionTimeout:3600000}")	
	private String reliableConditionTimeout;
	
	@Bean
	public DecayingTimeoutCondition generalTimeoutCondition()
	{
		return new DecayingTimeoutCondition(Integer.parseInt(generalConditionTimeout));
	}
	
	@Bean
	public DecayingTimeoutCondition reliableTimeoutCondition()
	{
		return new DecayingTimeoutCondition(Integer.parseInt(reliableConditionTimeout));
	}
	
	@Bean
	public VariableTimeoutCondition varaiableTimeoutCondition()
	{
		return new VariableTimeoutCondition(generalTimeoutCondition(), reliableTimeoutCondition());
	}	
	
	@Bean 
	public GeneralCompletionCondition generalCompletionCondition()
	{
		return new GeneralCompletionCondition();
	}
	
	@Bean
	public TimelyAndReliableCompletionCondition reliableCompletionCondition()
	{
		final TimelyAndReliableCompletionCondition retVal = new TimelyAndReliableCompletionCondition();
		retVal.setReceivedNotificationRepository(recRepo);
		
		return retVal;
	}
	
	@Bean
	public VariableCompletionCondition variableCompletionCondition()
	{
		return new VariableCompletionCondition(reliableCompletionCondition(), generalCompletionCondition());
	}
	
	@Bean 
	public BasicTxAggregator aggregationStrategy()
	{
		return new BasicTxAggregator(variableCompletionCondition(), varaiableTimeoutCondition());
	}
	
	@Bean 
	public MessageIdCorrelationExpression msgIdCorrelator()
	{
		return new MessageIdCorrelationExpression();
	}
}
