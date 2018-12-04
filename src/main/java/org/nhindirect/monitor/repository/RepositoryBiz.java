package org.nhindirect.monitor.repository;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.monitor.entity.ReceivedNotification;

/**
 * Utility business functions on top of repository interfaces.
 * @author gm2552
 * @Since 6.0
 */
public class RepositoryBiz
{
	@SuppressWarnings("deprecation")
	private static final Log LOGGER = LogFactory.getFactory().getInstance(RepositoryBiz.class);
	
	public static void addMessageToDuplicateStore(String messageId, String address, ReceivedNotificationRepository recRepo)
	{
		if (recRepo != null)
		{

			final List<String> notification = 
					recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(messageId.toUpperCase(), Arrays.asList(address.toUpperCase()));
			
			if (!notification.isEmpty())
			{
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Notification for message id " + messageId + " and address " + address +
						" already received.  Not adding to received notification store.");
				return;
			}
			
			final ReceivedNotification notif = new ReceivedNotification();
			notif.setMessageid(messageId);
			notif.setAddress(address);
			notif.setReceivedTime(Calendar.getInstance(Locale.getDefault()));			
			
			recRepo.save(notif);
		}
	}
}
