package org.nhindirect.monitor.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.nhindirect.monitor.condition.TxCompletionCondition;
import org.nhindirect.monitor.condition.TxTimeoutCondition;

import static org.mockito.Mockito.mock;

public class BasicTxAggregator_constructionTest 
{
	@Test
	public void costructAggregator()
	{
		TxTimeoutCondition timoutCondition = mock(TxTimeoutCondition.class);
		TxCompletionCondition condition = mock(TxCompletionCondition.class);
		
		BasicTxAggregator aggr = new BasicTxAggregator(condition, timoutCondition);

		assertNotNull(aggr);
		
		assertEquals(timoutCondition, aggr.timeoutCondition);
		assertEquals(condition, aggr.completionCondition);
	}
}
