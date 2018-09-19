package org.nhindirect.monitor.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.BaseTestPlan;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;


public class TxsResource_addTxWebTest extends SpringBaseTest
{
	@Autowired
	protected CamelContext context;
	
	abstract class TestPlan extends BaseTestPlan 
	{
		protected MockEndpoint mockEndpoint = null;
		
		@Override
		protected void setupMocks()
		{
			try
			{
								
				mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
		}
		
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
			
			Collection<Tx> txs = getTxsToSubmit();
			if (txs != null)
				txs.forEach(tx -> 
			    {
			    	HttpEntity<Tx> requestEntity = new HttpEntity<>(tx);
			    	final ResponseEntity<Void> resp = testRestTemplate.exchange("/txs", HttpMethod.POST, requestEntity, Void.class);
			    	
					if (resp.getStatusCodeValue() != 201)
						throw new HttpClientErrorException(resp.getStatusCode());	
			    });

			doAssertions(mockEndpoint);
		}
		
		
		protected void doAssertions(MockEndpoint mock) throws Exception
		{
			
		}
	}	
	
	@Test
	public void testSingleRecipMDNReceived_assertConditionComplete() throws Exception
	{
		new TestPlan()
		{
			
			@Override
			protected Collection<Tx> getTxsToSubmit()
			{
				Collection<Tx> txs = new ArrayList<Tx>();
				
				// send original message
				final String originalMessageId = UUID.randomUUID().toString();	
				
				Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
				txs.add(originalMessage);

				// send MDN to original message
				Tx mdnMessage = TestUtils.makeMessage(TxMessageType.MDN, UUID.randomUUID().toString(), originalMessageId, "gm2552@direct.securehealthemail.com", 
						"gm2552@cerner.com", "gm2552@direct.securehealthemail.com");
				txs.add(mdnMessage);
				
				return txs;

			}
			
			
			protected void doAssertions(MockEndpoint mock) throws Exception
			{
				List<Exchange> exchanges = mock.getReceivedExchanges();
				
				assertEquals(1, exchanges.size());
				
				@SuppressWarnings("unchecked")
				Collection<Tx> txs = (Collection<Tx>)exchanges.iterator().next().getIn().getBody();
				
				assertEquals(2, txs.size());
			}
		}.perform();		
	}
}
