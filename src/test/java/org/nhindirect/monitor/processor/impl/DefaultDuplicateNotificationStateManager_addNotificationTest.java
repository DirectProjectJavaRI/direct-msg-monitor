package org.nhindirect.monitor.processor.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Matchers.any;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
import org.nhindirect.monitor.processor.DuplicateNotificationStateManagerException;
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
public class DefaultDuplicateNotificationStateManager_addNotificationTest 
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
	public void testAddNotification_nullDAO_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		boolean execptionOccured = false;
		
		try
		{
			mgr.addNotification(mock(Tx.class));
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testAddNotification_nullTx_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		boolean execptionOccured = false;
		
		try
		{
			mgr.addNotification(null);
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testAddNotification_nonNotificationTx_assertTxNotAdded() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "1234", "", "", "", "gm2552@cerner.com");

		mgr.addNotification(tx);
		
		List<String> addedAddr = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", Arrays.asList("gm2552@cerner.com".toUpperCase()));
		assertEquals(0, addedAddr.size());
	}
	
	@Test
	public void testAddNotification_noOrigMessageId_assertTxNotAdded() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "", "", "", "gm2552@cerner.com");

		mgr.addNotification(tx);
		
		List<String> addedAddr = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", Arrays.asList("gm2552@cerner.com".toUpperCase()));
		assertEquals(0, addedAddr.size());
	}
	
	@Test
	public void testAddNotification_noFinalRecips_assertTxNotAdded() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "5678", "", "", "");

		mgr.addNotification(tx);
		
		List<String> addedAddr = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("1234", Arrays.asList("gm2552@cerner.com".toUpperCase()));
		assertEquals(0, addedAddr.size());
	}
	
	@Test
	public void testAddNotification_singleFinalRecipAdded_assertTxAdded() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "5678", "", "", "gm2552@cerner.com");

		mgr.addNotification(tx);
		
		List<String> addedAddr = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("5678", Arrays.asList("gm2552@cerner.com".toUpperCase()));
		assertEquals(1, addedAddr.size());
	}
	
	@Test
	public void testAddNotification_mulitpleFinalRecipAdded_assertTxAdded() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "5678", "", "", "gm2552@cerner.com,ah4626@cerner.com");

		mgr.addNotification(tx);
		
		List<String> addedAddr = recRepo.findByMessageidIgnoreCaseAndAddressInIgnoreCase("5678", 
				Arrays.asList("gm2552@cerner.com".toUpperCase(), "ah4626@cerner.com".toUpperCase()));
		assertEquals(2, addedAddr.size());
	}
	
	@Test
	public void testAddNotification_daoError_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		boolean execptionOccured = false;
		
		ReceivedNotificationRepository spyDao = mock(ReceivedNotificationRepository.class);
		doThrow(new RuntimeException("")).when(spyDao).save((ReceivedNotification)any());
		mgr.setReceivedNotificationRepository(spyDao);
		
		try
		{
			Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "5678", "", "", "gm2552@cerner.com,ah4626@cerner.com");

			mgr.addNotification(tx);
		}
		catch (DuplicateNotificationStateManagerException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
}
