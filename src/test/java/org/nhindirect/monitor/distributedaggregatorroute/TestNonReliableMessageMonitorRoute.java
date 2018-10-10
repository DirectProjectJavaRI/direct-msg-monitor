package org.nhindirect.monitor.distributedaggregatorroute;

import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestNonReliableMessageMonitorRoute extends org.nhindirect.monitor.route.TestNonReliableMessageMonitorRoute 
{
	@SuppressWarnings("deprecation")
	@Override
	public void postProcessTest() throws Exception
	{
		super.postProcessTest();
		
		final AggregationRepository aggRepo = context.getRegistry().lookupByType(AggregationRepository.class).values().iterator().next();
		final AggregationCompletedRepository aggCompRepo = context.getRegistry().lookupByType(AggregationCompletedRepository.class).values().iterator().next();
		
		aggRepo.deleteAll();
		aggCompRepo.deleteAll();
		
		assertEquals(0,aggRepo.findAllKeys().size());
		assertEquals(0,aggCompRepo.findAllKeys().size());
	}
	
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() 
    {
    	return new ClassPathXmlApplicationContext("distributedAggregatorRoutes/monitor-route-to-mock.xml");
    }
}
