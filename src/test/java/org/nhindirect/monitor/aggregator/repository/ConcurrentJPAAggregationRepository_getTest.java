package org.nhindirect.monitor.aggregator.repository;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import org.nhindirect.monitor.util.TestUtils;


@RunWith(CamelSpringBootRunner.class)
@DataJpaTest
@Transactional
@ContextConfiguration(classes=TestApplication.class)
@DirtiesContext
@ActiveProfiles("producerMock")
public class ConcurrentJPAAggregationRepository_getTest extends CamelSpringTestSupport 
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
	public void testGet_emptyRepository_assertNull() throws Exception
	{
	
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		assertNull(repo.get(context, "12345"));
	}
	
	@Test
	public void testGet_exchangeInRepository_txBody_assertExchangeFound() throws Exception
	{
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(tx);
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		final Exchange ex = repo.get(context, "12345");
		assertNotNull(ex);
		final Tx retrievedTx = (Tx)ex.getIn().getBody();
		assertEquals("12345", retrievedTx.getDetail(TxDetailType.MSG_ID).getDetailValue());
		final Integer version = (Integer)ex.getProperty(ConcurrentJPAAggregationRepository.AGGREGATION_ENTITY_VERSON);
		assertEquals(1, version.intValue());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testGet_exchangeInRepository_txCollectionBody_assertExchangeFound() throws Exception
	{
		final Tx tx1 = TestUtils.makeMessage(TxMessageType.IMF, "12345", "", "me@test.com", "you@test.com", "", "", "");
		final Tx tx2 = TestUtils.makeMessage(TxMessageType.IMF, "67890", "", "me@test2.com", "you@test2.com", "", "", "");
		
		final Collection<Tx> txs = Arrays.asList(tx1, tx2);
		
		final Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(txs);
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		
		repo.add(context, "12345", exchange);
		
		final Exchange ex = repo.get(context, "12345");
		assertNotNull(ex);
		

		final Collection<Tx> retrievedTxs = (Collection<Tx>)ex.getIn().getBody();
		assertEquals(2, retrievedTxs.size());
		
		assertEquals("12345", retrievedTxs.iterator().next().getDetail(TxDetailType.MSG_ID).getDetailValue());
	}
	
	@Test
	public void testGet_daoException_assertException() throws Exception
	{
		AggregationCompletedRepository dao = mock(AggregationCompletedRepository.class);
		doThrow(new RuntimeException()).when(dao).findById((String)any());
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, dao, 120);
		
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
	
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() 
    {
    	return new ClassPathXmlApplicationContext("distributedAggregatorRoutes/mock-route.xml");
    }
}
