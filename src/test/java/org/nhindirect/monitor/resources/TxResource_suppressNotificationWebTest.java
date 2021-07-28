package org.nhindirect.monitor.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.util.TestUtils;

public class TxResource_suppressNotificationWebTest extends SpringBaseTest
{
	@Test
	public void suppressNotificationWebTest() throws Exception
	{

		final Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "12345", "", "", "", "");
		
		final Boolean b = webClient.post().uri("/txs/suppressNotification")
			.bodyValue(tx).retrieve()
			.bodyToMono(Boolean.class).block();


		assertFalse(b);

	}	
}
