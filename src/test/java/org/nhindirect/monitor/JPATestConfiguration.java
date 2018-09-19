package org.nhindirect.monitor;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.nhindirect.monitor.aggregator", "org.nhindirect.monitor.dao"})
@EnableAutoConfiguration
public class JPATestConfiguration
{

}
