package org.nhindirect.monitor.autoconfig;

import org.nhindirect.monitor.aggregator.BasicTxAggregator;
import org.nhindirect.monitor.condition.impl.DecayingTimeoutCondition;
import org.nhindirect.monitor.condition.impl.GeneralCompletionCondition;
import org.nhindirect.monitor.condition.impl.TimelyAndReliableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableCompletionCondition;
import org.nhindirect.monitor.condition.impl.VariableTimeoutCondition;
import org.nhindirect.monitor.expression.MessageIdCorrelationExpression;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ConditionAutoConfiguration
{
	
	@Value("${direct.msgmonitor.condition.generalConditionTimeout:3600000}")	
	private String generalConditionTimeout;
	
	@Value("${direct.msgmonitor.condition.reliableConditionTimeout:3600000}")	
	private String reliableConditionTimeout;
	
	@ConditionalOnMissingBean
	@Bean
	DecayingTimeoutCondition generalTimeoutCondition()
	{
		return new DecayingTimeoutCondition(Integer.parseInt(generalConditionTimeout));
	}
	
	@ConditionalOnMissingBean
	@Bean
    DecayingTimeoutCondition reliableTimeoutCondition()
	{
		return new DecayingTimeoutCondition(Integer.parseInt(reliableConditionTimeout));
	}
	
	@ConditionalOnMissingBean
	@Bean
	VariableTimeoutCondition varaiableTimeoutCondition()
	{
		return new VariableTimeoutCondition(generalTimeoutCondition(), reliableTimeoutCondition());
	}	
	
	@ConditionalOnMissingBean
	@Bean 
	GeneralCompletionCondition generalCompletionCondition()
	{
		return new GeneralCompletionCondition();
	}
	
	@ConditionalOnMissingBean
	@Bean
	TimelyAndReliableCompletionCondition reliableCompletionCondition(ReceivedNotificationRepository recRepo)
	{
		final TimelyAndReliableCompletionCondition retVal = new TimelyAndReliableCompletionCondition();
		retVal.setReceivedNotificationRepository(recRepo);
		
		return retVal;
	}
	
	@ConditionalOnMissingBean
	@Bean
	VariableCompletionCondition variableCompletionCondition(TimelyAndReliableCompletionCondition reliableCompletionCondition, 
			GeneralCompletionCondition generalCompletionCondition)
	{
		return new VariableCompletionCondition(reliableCompletionCondition, generalCompletionCondition);
	}
	
	@ConditionalOnMissingBean
	@Bean 
	BasicTxAggregator aggregationStrategy(VariableCompletionCondition variableCompletionCondition, 
			VariableTimeoutCondition variableTimeoutCondition)
	{
		return new BasicTxAggregator(variableCompletionCondition, variableTimeoutCondition);
	}
	
	@ConditionalOnMissingBean
	@Bean 
	MessageIdCorrelationExpression msgIdCorrelator()
	{
		return new MessageIdCorrelationExpression();
	}
}
