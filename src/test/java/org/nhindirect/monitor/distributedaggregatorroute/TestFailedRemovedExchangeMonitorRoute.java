package org.nhindirect.monitor.distributedaggregatorroute;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "camel.springboot.xmlRoutes=classpath:distributedAggregatorRoutes/monitor-route-to-mock-removeexchange-error.xml")
public class TestFailedRemovedExchangeMonitorRoute extends TestFailedAddUpdateExchangeMonitorRoute
{

}
