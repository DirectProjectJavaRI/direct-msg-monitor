package org.nhindirect.monitor.resources;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class TxResource_suppressNotificationWebTest extends SpringBaseTest
{
	@Test
	public void suppressNotificationWebTest() throws Exception
	{

		final Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "12345", "", "", "", "");
		
		HttpEntity<Tx> requestEntity = new HttpEntity<>(tx);
		final ResponseEntity<Boolean> resp = testRestTemplate.exchange("/txs/suppressNotification", HttpMethod.POST, requestEntity, Boolean.class);
		if (resp.getStatusCodeValue() != 200)
			throw new HttpClientErrorException(resp.getStatusCode());		

		Boolean b = resp.getBody();

		assertFalse(b);

	}	
}
