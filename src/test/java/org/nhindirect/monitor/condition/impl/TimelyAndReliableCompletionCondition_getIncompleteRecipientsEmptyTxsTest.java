package org.nhindirect.monitor.condition.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.util.TestUtils;

public class TimelyAndReliableCompletionCondition_getIncompleteRecipientsEmptyTxsTest 
{
	@Test
	public void testIsComplete_nullTxs_assertEmptyList()
	{
		TimelyAndReliableCompletionCondition condition = new TimelyAndReliableCompletionCondition();
		
		Collection<String> recips = condition.getIncompleteRecipients(null);
		
		assertEquals(0, recips.size());
	}
	
	@Test
	public void testIsComplete_nullTxs_emptyTxs()
	{
		TimelyAndReliableCompletionCondition condition = new TimelyAndReliableCompletionCondition();
		
		Collection<String> recips = condition.getIncompleteRecipients(new ArrayList<Tx>());
		
		assertEquals(0, recips.size());
	}
	
	@Test
	public void testIsComplete_noMessageToTrack_emptyTxs()
	{
		TimelyAndReliableCompletionCondition condition = new TimelyAndReliableCompletionCondition();
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.DSN, "", UUID.randomUUID().toString(), "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "");
		Collection<Tx> txs = new ArrayList<Tx>();
		txs.add(originalMessage);
		
		Collection<String> recips = condition.getIncompleteRecipients(txs);
		
		assertEquals(0, recips.size());
	}
	
	@Test
	public void testIsComplete_noRecips_emptyTxs()
	{
		TimelyAndReliableCompletionCondition condition = new TimelyAndReliableCompletionCondition();
		
		Tx originalMessage = TestUtils.makeMessage(TxMessageType.IMF, "", UUID.randomUUID().toString(), "gm2552@cerner.com", "", "");
		Collection<Tx> txs = new ArrayList<Tx>();
		txs.add(originalMessage);
		
		Collection<String> recips = condition.getIncompleteRecipients(txs);
		
		assertEquals(0, recips.size());
	}
}
