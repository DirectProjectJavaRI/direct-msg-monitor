package org.nhindirect.monitor.aggregator.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.TestApplication;
import org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import org.nhindirect.monitor.util.TestUtils;


@RunWith(CamelSpringBootRunner.class)
@ContextConfiguration(classes=TestApplication.class)
@DirtiesContext
@ActiveProfiles("producerMock")
public class ConcurrentJPAAggregationRepository_removeTest extends CamelSpringTestSupport 
{
	@Autowired
	private AggregationRepository aggRepo;
	
	@Autowired
	private AggregationCompletedRepository aggCompRepo;
	
	@Before
	public void setUp() throws Exception
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
	public void testRemove_exchangeNotInRepo_assertException()
	{
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(tx);
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		boolean exceptionOccured = false;
		
		try
		{
			repo.remove(context, "12345", exchange);
		}
		catch (RuntimeException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testRemove_exchangeInRepo_assertRemovedAndCompletedAdded()
	{
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.setExchangeId(UUID.randomUUID().toString());

		
		
		exchange.getIn().setBody(tx);
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		repo.remove(context, "12345", exchange);
		
		final Exchange ex = repo.get(context, "12345");
		
		assertNull(ex);
		
		final Exchange completedExchange = repo.recover(context, exchange.getExchangeId());
		assertNotNull(completedExchange);
		final Tx completedTx = (Tx)completedExchange.getIn().getBody();
		assertEquals("12345", completedTx.getDetail(TxDetailType.MSG_ID).getDetailValue());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRemove_exchangeWithCollectionBodyInRepo_assertRemovedAndCompletedAdded()
	{
		final Tx tx1 = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Tx tx2 = TestUtils.makeMessage(TxMessageType.IMF, "67890", "", "me@test2.com", "you@test2.com", "", "", "");
		
		final Collection<Tx> txs = Arrays.asList(tx1, tx2);
		
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(txs);
		
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		repo.remove(context, "12345", exchange);
		
		assertNull(repo.get(context, "12345"));
		
		final Exchange completedExchange = repo.recover(context, exchange.getExchangeId());
		assertNotNull(completedExchange);
		

		final Collection<Tx> retrievedTxs = (Collection<Tx>)completedExchange.getIn().getBody();
		assertEquals(2, retrievedTxs.size());
		
		assertEquals("12345", retrievedTxs.iterator().next().getDetail(TxDetailType.MSG_ID).getDetailValue());
	}
	
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() 
    {
    	return new ClassPathXmlApplicationContext("distributedAggregatorRoutes/mock-route.xml");
    }
	
}
