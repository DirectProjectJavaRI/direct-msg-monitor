package org.nhindirect.monitor.repository;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.monitor.entity.PendingNotification;
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

	public static void addMessageToPendingNotificationStore(String dsnMessageId, String originalMessageId, String address,
			PendingNotificationRepository pendingRepo)
	{
		if (pendingRepo != null)
		{

			final List<String> notification =
					pendingRepo.findByDsnMessageIdIgnoreCaseAndAddressInIgnoreCase(dsnMessageId.toUpperCase(), Arrays.asList(address.toUpperCase()));

			if (!notification.isEmpty())
			{
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Pending notification for DSN message id " + dsnMessageId + " and address " + address +
						" already exists.  Not adding to pending notification store.");
				return;
			}

			final PendingNotification notif = new PendingNotification();
			notif.setDsnMessageId(dsnMessageId);
			notif.setOriginalMessageId(originalMessageId);
			notif.setAddress(address);
			notif.setCreatedTime(Calendar.getInstance(Locale.getDefault()));

			pendingRepo.save(notif);
		}
	}

	/**
	 * Promotes any pending notification entries for the given generated DSN's message id to the duplicate
	 * notification store, and removes them from the pending notification store.
	 * @param dsnMessageId The message id of the generated DSN.
	 * @param recRepo The duplicate notification repository.
	 * @param pendingRepo The pending notification repository.
	 * @return true if one or more pending entries were found (and promoted) for the given DSN message id; false otherwise.
	 */
	public static boolean promotePendingNotifications(String dsnMessageId, ReceivedNotificationRepository recRepo,
			PendingNotificationRepository pendingRepo)
	{
		if (recRepo == null || pendingRepo == null)
			return false;

		final List<PendingNotification> pending = pendingRepo.findByDsnMessageIdIgnoreCase(dsnMessageId.toUpperCase());

		if (pending.isEmpty())
			return false;

		for (PendingNotification notif : pending)
			addMessageToDuplicateStore(notif.getOriginalMessageId(), notif.getAddress(), recRepo);

		pendingRepo.deleteByDsnMessageIdIgnoreCase(dsnMessageId.toUpperCase());

		return true;
	}
}
