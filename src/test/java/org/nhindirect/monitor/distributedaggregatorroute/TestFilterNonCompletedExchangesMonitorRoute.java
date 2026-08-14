package org.nhindirect.monitor.distributedaggregatorroute;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "camel.springboot.routes-include-pattern=classpath:distributedAggregatorRoutes/monitor-route-to-mock-with-complete-filter.xml")
public class TestFilterNonCompletedExchangesMonitorRoute extends org.nhindirect.monitor.route.TestFilterNonCompletedExchangesMonitorRoute 
{

}

