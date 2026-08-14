package org.nhindirect.monitor.autoconfig;

import org.nhindirect.monitor.processor.impl.DefaultDuplicateNotificationStateManager;
import org.nhindirect.monitor.processor.impl.TimeoutDupStateManager;
import org.nhindirect.monitor.repository.PendingNotificationRepository;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ProcessorAutoConfiguration
{		
	@Autowired
	protected ReceivedNotificationRepository recRepo;

	@Autowired
	protected PendingNotificationRepository pendingRepo;

	@Value("${direct.msgmonitor.dupStateDAO.retensionTime:7}")
	private String messageRetention;

	@Value("${direct.msgmonitor.pendingStateDAO.retensionTime:24}")
	private String pendingMessageRetention;


	@Value("${direct.msgmonitor.dsnSender.exchange:notifications}")	
	private String dsnSenderExchange;
	
	@Value("${direct.msgmonitor.dsnSender.routing-key:notifications}")	
	private String dsnSenderRoutingKey;
	
	@ConditionalOnMissingBean
	@Bean
	DefaultDuplicateNotificationStateManager duplicationStateManager()
	{
		final TimeoutDupStateManager retVal = new TimeoutDupStateManager();
		
		retVal.setReceivedNotificationRepository(recRepo);
		retVal.setPendingNotificationRepository(pendingRepo);
		retVal.setMessageRetention(Integer.parseInt(messageRetention));
		retVal.setPendingMessageRetention(Integer.parseInt(pendingMessageRetention));

		return retVal;
	}
}
