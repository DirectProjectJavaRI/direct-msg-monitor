package org.nhindirect.monitor.springconfig;

import org.nhindirect.monitor.processor.impl.DefaultDuplicateNotificationStateManager;
import org.nhindirect.monitor.processor.impl.TimeoutDupStateManager;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig
{		
	@Autowired
	protected ReceivedNotificationRepository recRepo;
	
	@Value("${direct.msgmonitor.dupStateDAO.retensionTime:7}")	
	private String messageRetention;
	
	@Value("${direct.msgmonitor.dsnSender.exchange:notifications}")	
	private String dsnSenderExchange;
	
	@Value("${direct.msgmonitor.dsnSender.routing-key:notifications}")	
	private String dsnSenderRoutingKey;
	
	@Bean
	public DefaultDuplicateNotificationStateManager duplicationStateManager()
	{
		final TimeoutDupStateManager retVal = new TimeoutDupStateManager();
		
		retVal.setReceivedNotificationRepository(recRepo);
		retVal.setMessageRetention(Integer.parseInt(messageRetention));
				
		return retVal;
	}
}
