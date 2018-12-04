package org.nhindirect.monitor.resources;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.monitor.BaseTestPlan;
import org.nhindirect.monitor.SpringBaseTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


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
			final ResponseEntity<String> resp = testRestTemplate.exchange("/health", HttpMethod.GET, null, String.class);
			if (resp.getStatusCodeValue() != 200)
				throw new HttpClientErrorException(resp.getStatusCode());

			doAssertions(resp.getBody());
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
