package org.nhindirect.monitor.springconfig;

import org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository;
import org.nhindirect.monitor.dao.AggregationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AggregatorRepositoryConfig
{
	@Autowired
	protected AggregationDAO aggDao;
	
	@Value("${direct.msgmonitor.recovery.retryInterval:30000}")	
	private String retryInterval;
	
	@Value("${direct.msgmonitor.recovery.maxRetryAttemps:12}")	
	private String maxRetryAttemps;
	
	@Value("${direct.msgmonitor.recovery.deadLetterUri:file:recovery/directMonitorDeadLetter}")	
	private String deadLetterUri;
	
	@Bean
	public ConcurrentJPAAggregationRepository directMonitoringRepo()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository();
		
		repo.setAggreationDAO(aggDao);
		repo.setRecoveryInterval(Integer.parseInt(retryInterval));
		repo.setMaximumRedeliveries(Integer.parseInt(maxRetryAttemps));
		repo.setDeadLetterUri(deadLetterUri);
		
		return repo;
	}
}
