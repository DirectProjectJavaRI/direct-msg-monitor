package org.nhindirect.monitor.aggregator.repository;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("producerMock")
public class ConcurrentJPAAggregationRepository_getKeysTest extends SpringBaseTest 
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
	public void testGetKeys_emptyRepository_assertEmptyList()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		final Set<String> keys = repo.getKeys();
		
		assertEquals(0, keys.size());
	}
	
	@Test
	public void testGetKeys_singleEntry_assertSingleKey()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
	
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(tx);
		
		repo.add(context, "12345", exchange);
		
		final Set<String> keys = repo.getKeys();
		
		assertEquals(1, keys.size());
		assertEquals("12345", keys.iterator().next());
	}
	
	@Test
	public void testGetKeys_multipleEntries_assertMultipleKeys()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
	
		final Tx tx1 = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange1 = new DefaultExchange(context);
		exchange1.getIn().setBody(tx1);
		
		repo.add(context, "12345", exchange1);
		
		final Tx tx2 = TestUtils.makeMessage(TxMessageType.IMF, "123456", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange2 = new DefaultExchange(context);
		exchange2.getIn().setBody(tx2);
		
		repo.add(context, "123456", exchange2);
		
		final Set<String> keys = repo.getKeys();
		
		assertEquals(2, keys.size());
		Iterator<String> iter = keys.iterator();
		
		assertEquals("12345", iter.next());
		assertEquals("123456", iter.next());
	}
	
	@Test
	public void testGetKeys_daoException_assertException() throws Exception
	{
		AggregationRepository dao = mock(AggregationRepository.class);
		doThrow(new RuntimeException()).when(dao).findAllKeys();
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(dao, aggCompRepo, 120);
		
		boolean exceptionOccured = false;
		try
		{
			repo.getKeys();
		}
		catch(RuntimeException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}	
}
