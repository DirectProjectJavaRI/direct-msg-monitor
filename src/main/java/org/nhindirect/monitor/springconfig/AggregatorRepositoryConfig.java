package org.nhindirect.monitor.springconfig;

import org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("org.nhindirect.monitor.repository")
public class AggregatorRepositoryConfig
{
	@Autowired
	protected AggregationRepository aggRepo;
	
	@Autowired
	protected AggregationCompletedRepository aggCompRepo;
	
	@Value("${direct.msgmonitor.recovery.retryInterval:30000}")	
	private String retryInterval;
	
	@Value("${direct.msgmonitor.recovery.maxRetryAttemps:12}")	
	private String maxRetryAttemps;
	
	@Value("${direct.msgmonitor.recovery.deadLetterUri:file:recovery/directMonitorDeadLetter}")	
	private String deadLetterUri;
	
    @Value("${monitor.aggregatorRepository.recoveryLockInterval:120}")
    protected int recoveredEntityLockInterval;
	
	@Bean
	public ConcurrentJPAAggregationRepository directMonitoringRepo()
	{
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository();
		
		repo.setAggreationRepository(aggRepo);
		repo.setAggreationCompletedRepository(aggCompRepo);
		repo.setRecoveryInterval(Integer.parseInt(retryInterval));
		repo.setMaximumRedeliveries(Integer.parseInt(maxRetryAttemps));
		repo.setDeadLetterUri(deadLetterUri);
		repo.setRecoveredEntityLockInterval(recoveredEntityLockInterval);
		
		return repo;
	}
}
