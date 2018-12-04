package org.nhindirect.monitor.streams;

import static org.mockito.Mockito.mock;

import org.apache.camel.ProducerTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("producerMock")
@Import(TxEventSink.class)
public class TxTestConfiguration
{	
	@ConditionalOnMissingBean
	@Bean 
	public ProducerTemplate producer()
	{
		return mock(ProducerTemplate.class);
	}
}
