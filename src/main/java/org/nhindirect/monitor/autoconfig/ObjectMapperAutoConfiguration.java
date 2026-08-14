package org.nhindirect.monitor.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
public class ObjectMapperAutoConfiguration
{
	@ConditionalOnMissingBean
	@Bean
	ObjectMapper objectMapper()
	{
		return new ObjectMapper();
	}
}
