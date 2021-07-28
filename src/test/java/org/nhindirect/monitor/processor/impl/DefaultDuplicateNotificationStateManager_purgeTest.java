package org.nhindirect.monitor.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nhindirect.monitor.TestApplication;
import org.nhindirect.monitor.entity.ReceivedNotification;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional
@ContextConfiguration(classes=TestApplication.class)
@ActiveProfiles("producerMock")
public class DefaultDuplicateNotificationStateManager_purgeTest 
{
	@Autowired
	private ReceivedNotificationRepository recRepo;
	
	@BeforeEach
	public void setUp() throws Exception
	{
		Calendar qualTime = Calendar.getInstance(Locale.getDefault());
		qualTime.add(Calendar.YEAR, 10);
		
		recRepo.deleteByReceivedTimeBefore(qualTime);
	}
	
	@Test
	public void testPurge_nullDAO_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		boolean execptionOccured = false;
		
		try
		{
			mgr.purge();
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testPurgeNotification_notificationNotYetPurgable_assertNotPurged() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		// set one day ago.... messages are all new, so should not be purgable
		mgr.setMessageRetention(1);
		
		final String messageId = UUID.randomUUID().toString();
		
		ReceivedNotification notif = new ReceivedNotification();
		notif.setAddress("gm2552@cerner.com");
		notif.setMessageid(messageId);
		notif.setReceivedTime(Calendar.getInstance(Locale.getDefault()));
		
		
		recRepo.save(notif);
		
		Collection<String> addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(messageId.toUpperCase(), Arrays.asList("gm2552@cerner.com".toUpperCase()));
		
		assertEquals(1, addresses.size());
		
		mgr.purge();
		
		addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(messageId.toUpperCase(), Arrays.asList("gm2552@cerner.com".toUpperCase()));
		
		assertEquals(1, addresses.size());	
	}
	
	@Test
	public void testPurgeNotification_notificationnotificationPurged_assertNotPurged() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		// set the purge time to tomorrow so every thing today will get purged
		mgr.setMessageRetention(-1);
		
		final String messageId = UUID.randomUUID().toString();
		
		ReceivedNotification notif = new ReceivedNotification();
		notif.setAddress("gm2552@cerner.com");
		notif.setMessageid(messageId);
		notif.setReceivedTime(Calendar.getInstance(Locale.getDefault()));
		
		
		recRepo.save(notif);
		
		Collection<String> addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(messageId.toUpperCase(), Arrays.asList("gm2552@cerner.com".toUpperCase()));
		
		assertEquals(1, addresses.size());
		
		mgr.purge();
		
		addresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase(messageId.toUpperCase(), Arrays.asList("gm2552@cerner.com".toUpperCase()));
		
		assertEquals(0, addresses.size());	
	}	
	
	public void testPurgeNotification_daoError_assertException() throws Exception
	{
		Assertions.assertThrows(RuntimeException.class, () ->
		{
			DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
			
			ReceivedNotificationRepository spyDao = mock(ReceivedNotificationRepository.class);
			doThrow(new RuntimeException("")).when(spyDao).deleteByReceivedTimeBefore((Calendar)any());
			mgr.setReceivedNotificationRepository(spyDao);
			
			mgr.purge();
		});
		
	}	
}
