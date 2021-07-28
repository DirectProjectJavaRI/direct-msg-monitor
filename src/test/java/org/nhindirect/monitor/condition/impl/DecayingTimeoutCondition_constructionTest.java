package org.nhindirect.monitor.condition.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DecayingTimeoutCondition_constructionTest 
{
	@Test
	public void testConstruction()
	{		
		DecayingTimeoutCondition cond = new DecayingTimeoutCondition(25);
		
		assertEquals(25, cond.completionTimeout);
	}
	
}
