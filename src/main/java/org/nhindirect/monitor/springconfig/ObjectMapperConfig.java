package org.nhindirect.monitor.springconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ObjectMapperConfig
{
	@ConditionalOnMissingBean
	@Bean
	public ObjectMapper objectMapper()
	{
		return new ObjectMapper();
	}
}
