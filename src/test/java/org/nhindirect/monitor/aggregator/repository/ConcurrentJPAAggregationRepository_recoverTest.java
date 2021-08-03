package org.nhindirect.monitor.aggregator.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import org.nhindirect.monitor.util.TestUtils;

@ActiveProfiles("producerMock")
public class ConcurrentJPAAggregationRepository_recoverTest extends SpringBaseTest 
{
	@Autowired
	private AggregationRepository aggRepo;
	
	@Autowired
	private AggregationCompletedRepository aggCompRepo;
	
	@Autowired
	private CamelContext context;
	
	@BeforeEach
	public void setUp()
	{
		super.setUp();
		
		aggRepo.deleteAll();
		aggCompRepo.deleteAll();
		
		List<String> keys = aggRepo.findAllKeys();
		assertEquals(0, keys.size());
		
		keys = aggCompRepo.findAllKeys();
		assertEquals(0, keys.size());
	}
	
	@Test
	public void testRecover_emptyRepository_assertNoRecovery()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		assertNull(repo.recover(context, "12345"));
	}
	
	@Test
	public void testRecover_exchangeInRepo_assertRecovered()
	{
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(tx);
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		repo.remove(context, "12345", exchange);
		
		final Exchange completedExchange = repo.recover(context, exchange.getExchangeId());
		assertNotNull(completedExchange);
		final Tx completedTx = (Tx)completedExchange.getIn().getBody();
		assertEquals("12345", completedTx.getDetail(TxDetailType.MSG_ID).getDetailValue());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRecover_exchangeWithCollectionBodyInRepo_assertRecovered()
	{
		final Tx tx1 = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Tx tx2 = TestUtils.makeMessage(TxMessageType.IMF, "67890", "", "me@test2.com", "you@test2.com", "", "", "");
		
		final Collection<Tx> txs = Arrays.asList(tx1, tx2);
		
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(txs);
		
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		repo.remove(context, "12345", exchange);
		
		final Exchange completedExchange = repo.recover(context, exchange.getExchangeId());
		assertNotNull(completedExchange);
		

		final Collection<Tx> retrievedTxs = (Collection<Tx>)completedExchange.getIn().getBody();
		assertEquals(2, retrievedTxs.size());
		
		assertEquals("12345", retrievedTxs.iterator().next().getDetail(TxDetailType.MSG_ID).getDetailValue());
	}
	
	@Test
	public void testRecover_daoException_assertException() throws Exception
	{
		AggregationCompletedRepository dao = mock(AggregationCompletedRepository.class);
		doThrow(new RuntimeException()).when(dao).findById((String)any());
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, dao, 120 );
		
		boolean exceptionOccured = false;
		try
		{
			repo.recover(context, "12345");
		}
		catch(RuntimeException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}	
}
