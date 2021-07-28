package org.nhindirect.monitor.aggregator.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;


public class ConcurrentJPAAggregationRepository_doStartTest 
{
	@Test
	public void testDoStart_emptyAggregation_assertNoException() throws Exception
	{
		AggregationRepository aggRepo = mock(AggregationRepository.class);
		AggregationCompletedRepository aggCompRepo = mock(AggregationCompletedRepository.class);
		when(aggRepo.findAllKeys()).thenReturn(new ArrayList<String>());
		when(aggCompRepo.findAllKeys()).thenReturn(new ArrayList<String>());
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		repo.doStart();
		repo.doStop();
	}
	
	@Test
	public void testDoStart_nonEmptyAggregation_assertNoException() throws Exception
	{
		AggregationRepository aggRepo = mock(AggregationRepository.class);
		AggregationCompletedRepository aggCompRepo = mock(AggregationCompletedRepository.class);
		when(aggRepo.findAllKeys()).thenReturn(Arrays.asList("12345"));
		when(aggCompRepo.findAllKeys()).thenReturn(Arrays.asList("12345"));
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository(aggRepo, aggCompRepo, 120);
		repo.doStart();
		repo.doStop();
	}
	
	@Test
	public void testDoStart_emptyDAO_assertException() throws Exception
	{
		
		final ConcurrentJPAAggregationRepository repo = new ConcurrentJPAAggregationRepository();
		
		boolean exceptionOccured = false;
		try
		{
			repo.doStart();
		}
		catch(RuntimeException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}	
}
