package org.nhindirect.monitor.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DSNMailSender_constructorTest 
{
	@Test
	public void testContrust_defaultConstructor()
	{
		SMTPDSNMailSender sender = new SMTPDSNMailSender();
		
		assertNull(sender.gatewayHost);
		assertNull(sender.clientFactory);
		assertEquals(25, sender.gatewayPort);
	}
	
	@Test
	public void testContrust_parameterizedConstructor()
	{
		SMTPClientFactory factory = new SMTPClientFactory();
		
		SMTPDSNMailSender sender = new SMTPDSNMailSender("smtp://localhost", factory);
		
		assertEquals("localhost", sender.gatewayHost);
		assertEquals(factory, sender.clientFactory);
		assertEquals(25, sender.gatewayPort);
	}	
	
	@Test
	public void testContrust_parameterizedConstructor_customPort()
	{
		SMTPClientFactory factory = new SMTPClientFactory();
		
		SMTPDSNMailSender sender = new SMTPDSNMailSender("smtp://localhost:10026", factory);
		
		assertEquals("localhost", sender.gatewayHost);
		assertEquals(factory, sender.clientFactory);
		assertEquals(10026, sender.gatewayPort);
	}
	
	@Test
	public void testContrust_parameterizedConstructor_invaludURL()
	{
		SMTPClientFactory factory = new SMTPClientFactory();
		
		boolean exceptionOccured = false;
		
		try
		{
			new SMTPDSNMailSender("smtpewdf://localhost\\:10026", factory);
		}
		catch (IllegalArgumentException e)
		{
			exceptionOccured = true;
		}
		assertTrue(exceptionOccured);
	}
}
