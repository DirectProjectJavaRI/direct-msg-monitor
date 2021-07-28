package org.nhindirect.monitor.distributedaggregatorroute;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "camel.springboot.xmlRoutes=classpath:distributedAggregatorRoutes/monitor-route-to-error-message-generator.xml")
public class TestNonCompletedToDSNGeneratorMonitorRoute extends org.nhindirect.monitor.route.TestNonCompletedToDSNGeneratorMonitorRoute 
{	
	
}