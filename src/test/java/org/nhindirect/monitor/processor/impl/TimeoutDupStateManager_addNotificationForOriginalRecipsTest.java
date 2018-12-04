package org.nhindirect.monitor.processor.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.TestApplication;
import org.nhindirect.monitor.entity.ReceivedNotification;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional
@ContextConfiguration(classes=TestApplication.class)
@ActiveProfiles("producerMock")
public class TimeoutDupStateManager_addNotificationForOriginalRecipsTest 
{
	@Autowired
	private ReceivedNotificationRepository recRepo;
	
	@Before
	public void setUp() throws Exception
	{
		Calendar qualTime = Calendar.getInstance(Locale.getDefault());
		qualTime.add(Calendar.YEAR, 10);
		
		recRepo.deleteByReceivedTimeBefore(qualTime);
	}
	
	@Test
	public void testAddNotificationForOriginalRecips_nullDAO_assertException() throws Exception
	{
		final TimeoutDupStateManager mgr = new TimeoutDupStateManager();
		boolean execptionOccured = false;
		
		try
		{
			mgr.addNotificationForOriginalRecips(new ArrayList<Tx>());
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_nullCollection_assertException() throws Exception
	{
		final TimeoutDupStateManager mgr = new TimeoutDupStateManager();
		boolean execptionOccured = false;
		
		mgr.setReceivedNotificationRepository(recRepo);
		
		try
		{
			mgr.addNotificationForOriginalRecips(null);
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testAddNotificationForOriginalRecips_noRecips_assertNotificationNotAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();

		ReceivedNotificationRepository dao = mock(ReceivedNotificationRepository.class);
		
		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "1234", "", "", "", "");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));
		
		verify(dao, never()).save((ReceivedNotification)any());
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_noOrigMsg_assertNotificationNotAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();

		ReceivedNotificationRepository dao = mock(ReceivedNotificationRepository.class);
		
		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "", "test@test.com", "me@you.com", "test@test.com");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));
		
		verify(dao, never()).save((ReceivedNotification)any());
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_addSingleRecip_nonReliable_assertNotificationsNotAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();
		
		ReceivedNotificationRepository dao = mock(ReceivedNotificationRepository.class);
		
		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "1234", "", "test@test.com", "me@you.com", "test@test.com");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));

		verify(dao, never()).save((ReceivedNotification)any());
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_addSingleRecip_assertNotificationsAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();
		
		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeReliableMessage(TxMessageType.IMF, "1234", "", "test@test.com", "me@you.com", "test@test.com", "", "");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));

		
		List<String> recAddresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", Arrays.asList("me@you.com".toUpperCase()));
		assertTrue(recAddresses.contains("me@you.com"));
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_addMultipleRecips_assertNotificationsAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();

		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeReliableMessage(TxMessageType.IMF, "1234", "", "test@test.com", "me@you.com,you@you.com", "test@test.com", "", "");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));
		
		List<String> recAddresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", 
				Arrays.asList("me@you.com".toUpperCase(), "you@you.com".toUpperCase()));
		assertTrue(recAddresses.contains("me@you.com"));
		assertTrue(recAddresses.contains("you@you.com"));
	}	
	
	@Test
	public void testAddNotificationForOriginalRecips_addDupRecip_assertNotificationsAdded() throws Exception
	{
		TimeoutDupStateManager mgr = new TimeoutDupStateManager();

		mgr.setReceivedNotificationRepository(recRepo);
		
		final Tx tx = TestUtils.makeReliableMessage(TxMessageType.IMF, "1234", "", "test@test.com", "me@you.com,you@you.com", "test@test.com", "", "");
				
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));
		
		List<String> recAddresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", 
				Arrays.asList("me@you.com".toUpperCase(), "you@you.com".toUpperCase()));
		assertTrue(recAddresses.contains("me@you.com"));
		assertTrue(recAddresses.contains("you@you.com"));

		// add it again
		mgr.addNotificationForOriginalRecips(Arrays.asList(tx));	
		
		recAddresses = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", 
				Arrays.asList("me@you.com".toUpperCase(), "you@you.com".toUpperCase()));
		assertEquals(2, recAddresses.size());
	}		
}
