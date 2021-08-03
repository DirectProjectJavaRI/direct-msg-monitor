package org.nhindirect.monitor.processor.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.TestApplication;
import org.nhindirect.monitor.processor.DuplicateNotificationStateManagerException;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional
@ContextConfiguration(classes=TestApplication.class)
@DirtiesContext
@ActiveProfiles("producerMock")
public class DefaultDuplicateNotificationStateManager_suppressNotificationTest 
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
	public void testSuppressNotification_nullDAO_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		boolean execptionOccured = false;
		
		try
		{
			mgr.suppressNotification(mock(Tx.class));
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testSuppressNotification_nullTx_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		boolean execptionOccured = false;
		
		try
		{
			mgr.suppressNotification(null);
		}
		catch (IllegalArgumentException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
	
	@Test
	public void testSuppressNotification_nonNotificationTx_assertFalse() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.IMF, "1234", "", "", "", "gm2552@cerner.com");

		assertFalse(mgr.suppressNotification(tx));
	}
	
	@Test
	public void testSuppressNotification_displayedDisposition_assertFalse() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "5678", "", "",
				"", "", MDNStandard.Disposition_Displayed);

		assertFalse(mgr.suppressNotification(tx));
	}
	
	@Test
	public void testSuppressNotification_noOrigMessageId_assertFalse() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "", "", "",
				"gm2552@cerner.com", "", MDNStandard.Disposition_Error);

		assertFalse(mgr.suppressNotification(tx));
	}
	
	@Test
	public void testSuppressNotification_noFinalRecip_assertFalse() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "5678", "", "",
				"", "", MDNStandard.Disposition_Error);

		assertFalse(mgr.suppressNotification(tx));
	}
	
	@Test
	public void testSuppressNotification_recipNotInStore_assertFalse() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "5678", "", "",
				"gm2552@cerner.com", "", MDNStandard.Disposition_Error);

		assertFalse(mgr.suppressNotification(tx));
	}
	
	@Test
	public void testSuppressNotification_recipInStore_assertTrue() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		mgr.setReceivedNotificationRepository(recRepo);
		
		Tx tx = TestUtils.makeMessage(TxMessageType.MDN, "1234", "5678", "", "",
				"gm2552@cerner.com", "", MDNStandard.Disposition_Error);

		mgr.addNotification(tx);
		
		assertTrue(mgr.suppressNotification(tx));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAddNotification_daoError_assertException() throws Exception
	{
		DefaultDuplicateNotificationStateManager mgr = new DefaultDuplicateNotificationStateManager();
		boolean execptionOccured = false;
		
		ReceivedNotificationRepository spyDao = mock(ReceivedNotificationRepository.class);
		doThrow(new RuntimeException("")).when(spyDao).findByMessageidIgnoreCaseAndAddressInIgnoreCase((String)any(), (List<String>)any());
		mgr.setReceivedNotificationRepository(spyDao);
		
		try
		{
			Tx tx = TestUtils.makeMessage(TxMessageType.DSN, "1234", "5678", "", "", "gm2552@cerner.com,ah4626@cerner.com");

			mgr.suppressNotification(tx);
		}
		catch (DuplicateNotificationStateManagerException e)
		{
			execptionOccured = true;
		}
		
		assertTrue(execptionOccured);
	}
}
