package org.nhindirect.monitor.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.monitor.processor.DuplicateNotificationStateManager;
import org.nhindirect.monitor.processor.DuplicateNotificationStateManagerException;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;

public class TxResource_suppressNotificationTest 
{
	@Test
	public void testSuppressNotification_nullDAO_assertExcecption()
	{
		Tx tx = mock(Tx.class);
		
		TxsResource resource = new TxsResource(null, null);
		
		boolean exceptionOccured = false;
		
		try
		{
			resource.supressNotification(tx);
		}
		catch (IllegalStateException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testSuppressNotification_suppressFalse_assertFalseAnd200StatusCode()
	{
		Tx tx = mock(Tx.class);
		DuplicateNotificationStateManager dupMgr = mock(DuplicateNotificationStateManager.class);
		
		TxsResource resource = new TxsResource(null, dupMgr);
		
		ResponseEntity<Mono<Boolean>> res = resource.supressNotification(tx);
		assertEquals(200, res.getStatusCodeValue());
		assertFalse((Boolean)res.getBody().block());
	}
	
	@Test
	public void testSuppressNotification_suppressTrue_assertTrueAnd200StatusCode() throws Exception
	{
		Tx tx = mock(Tx.class);
		DuplicateNotificationStateManager dupMgr = mock(DuplicateNotificationStateManager.class);
		when(dupMgr.suppressNotification(tx)).thenReturn(true);
		
		TxsResource resource = new TxsResource(null, dupMgr);
		
		ResponseEntity<Mono<Boolean>> res = resource.supressNotification(tx);
		assertEquals(200, res.getStatusCodeValue());
		assertTrue((Boolean)res.getBody().block());
	}
	
	@Test
	public void testSuppressNotification_mgrException_assert500StatusCode() throws Exception
	{
		Tx tx = mock(Tx.class);
		DuplicateNotificationStateManager dupMgr = mock(DuplicateNotificationStateManager.class);
		when(dupMgr.suppressNotification(tx)).thenThrow(new DuplicateNotificationStateManagerException());
		
		TxsResource resource = new TxsResource(null, dupMgr);
		
		ResponseEntity<Mono<Boolean>> res = resource.supressNotification(tx);
		assertEquals(500, res.getStatusCodeValue());
	}
}
