package org.nhindirect.monitor.resources;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.monitor.BaseTestPlan;
import org.nhindirect.monitor.SpringBaseTest;


public class HealthCheckResource_healthTest extends SpringBaseTest
{	
	abstract class TestPlan extends BaseTestPlan 
	{
		
		@Override
		protected void tearDownMocks()
		{
		}

		
		protected Collection<Tx> getTxsToSubmit()
		{
			return Collections.emptyList();
		}
									
		
		@Override
		protected void performInner() throws Exception
		{
			final String html = webClient.get().uri("/health").retrieve()
					.bodyToMono(String.class).block();

			doAssertions(html);
		}
		
		
		protected void doAssertions(String html) throws Exception
		{
			
		}
	}
	
	@Test
	public void testHealth_assertNoException() throws Exception
	{
		new TestPlan()
		{
		
			protected void doAssertions(String html) throws Exception
			{
				assertNotNull(html);
				assertFalse(html.isEmpty());
				assertTrue(html.startsWith("<"));
				
			}
		}.perform();		
	}
}
