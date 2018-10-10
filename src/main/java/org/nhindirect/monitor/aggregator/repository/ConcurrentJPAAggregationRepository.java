/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.monitor.aggregator.repository;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.hawtdb.HawtDBCamelCodec;
import org.apache.camel.spi.RecoverableAggregationRepository;
import org.apache.camel.support.ServiceSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.hawtbuf.Buffer;
import org.nhindirect.monitor.entity.Aggregation;
import org.nhindirect.monitor.entity.AggregationCompleted;
import org.nhindirect.monitor.repository.AggregationCompletedRepository;
import org.nhindirect.monitor.repository.AggregationRepository;
import org.nhindirect.monitor.repository.AggregationRepositoryException;
import org.nhindirect.monitor.repository.AggregationVersionException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * RecoverableAggregationRepository implementation that supports high concurrency of exchange flow.  This implementation is similar to the 
 * camel JdbcAggregationRepository, but uses JPA instead of SQL statements.  It also supports distribution of state when multiple instances of this
 * servicer are running in either multiple JVMs or across multiple nodes.  Lastly, it mitigates known issues in the JdbcAggregationRepository with 
 * multiple instances running and performing recovery at the same time by locking the recovery table for a configurable amount of time.
 * @author Greg Meyer
 * @since 1.1
 */
public class ConcurrentJPAAggregationRepository extends ServiceSupport implements RecoverableAggregationRepository
{
	@SuppressWarnings("deprecation")
	private static final Log LOGGER = LogFactory.getFactory().getInstance(ConcurrentJPAAggregationRepository.class);
	
	protected static final String AGGREGATION_ENTITY_VERSON = "AGGREGATION_ENTITY_VERSON";
	
	protected static final String AGGREGATION_COMPLETE_ENTITY_VERSON = "AGGREGATION_COMPLETE_ENTITY_VERSON";
    
	protected static final int DEFAULT_ENTITY_LOCK_INTERVAL = 120;
	
	protected int recoveredEntityLockInterval;
	
	protected HawtDBCamelCodec codec = new HawtDBCamelCodec();	
	protected AggregationRepository aggRepo;
	protected AggregationCompletedRepository aggCompRepo;
	protected long recoveryInterval = 5000;
	protected boolean useRecovery = true;
	protected int maximumRedeliveries;
	protected String deadLetterUri;
	
	/**
	 * Constructor
	 */
	public ConcurrentJPAAggregationRepository()
	{
		recoveredEntityLockInterval = DEFAULT_ENTITY_LOCK_INTERVAL;
	}
	
	/**
	 * Constructor with a DAO implementation
	 * @param aggRepo The underlying repository that maintains the exchanges.
	 * @param aggCompRepo The underlying repository that maintains the completed exchanges.
	 */
	public ConcurrentJPAAggregationRepository(AggregationRepository aggRepo, AggregationCompletedRepository aggCompRepo, int recoveredEntityLockInterval)
	{
		this.aggRepo = aggRepo;
		this.aggCompRepo = aggCompRepo;
	}
	
	/**
	 * Sets the aggregation repository that maintains the exchanges.
	 * @param aggRepo The underlying repository that maintains the exchanges.
	 */
	public void setAggreationRepository(AggregationRepository aggRepo)
	{
		this.aggRepo = aggRepo;
	}

	/**
	 * Sets the aggregation repository that maintains the completed exchanges.
	 * @param aggRepo The underlying repository that maintains the completed exchanges.
	 */
	public void setAggreationCompletedRepository(AggregationCompletedRepository aggCompRepo)
	{
		this.aggCompRepo = aggCompRepo;
	}
	
	public void setRecoveredEntityLockInterval(int recoveredEntityLockInterval)
	{
		this.recoveredEntityLockInterval = recoveredEntityLockInterval;
	}
	
	
	/**
	 * {@inheritDoc}
	 * This specific implementation throws a Runtime exception on DAO errors.  This implementation also checks for
	 * consistency/concurrency of the exchange.  If the attempted exchange does match the latest and greatest
	 * exchange version, then an AggregationVersionException is wrapped by the runtime error.  Routes using this
	 * repository should catch the AggregationVersionException and attempt to retry the exchange.  If exception handling
	 * and redelivery is configured correctly, Camel should automatically reload the exchange from the latest version in the 
	 * and attempt the aggregation process again.
	 * 
	 */
	@Override
	@Transactional
	public synchronized Exchange add(CamelContext camelContext, String key, Exchange exchange) 
	{
        try 
        {
        	// serialize the exchange to a blob
            final byte[] blob = codec.marshallExchange(camelContext, exchange, true).getData();
 
            // get the current version of the exchange... if this is the first time the exchange with the
            // given key is added, this should result in null
            Integer currentEntityVersion = (Integer)exchange.getProperty(AGGREGATION_ENTITY_VERSON);
            
            Aggregation agg = new Aggregation();
        	agg.setExchangeBlob(blob);
        	agg.setId(key);
        	agg.setVersion(currentEntityVersion == null ? 0 : currentEntityVersion);
        	
        	// add/update the repository... 
        	//dao.addUpdateAggregation(agg);
			// find the aggregation
			final Optional<Aggregation> existingAggrOpt = aggRepo.findById(key);
			
			// if its not there by the requested aggregation has a version > 1, then somethine is wrong
			if (!existingAggrOpt.isPresent() && agg.getVersion() > 0)
				throw new AggregationVersionException("Aggregation not found but expected to exist due to non 0 version number");
        	
			if (existingAggrOpt.isPresent())
			{
				final Aggregation existingAggr = existingAggrOpt.get();
				
				// make sure the version on the existing aggregator matches ours
				if (existingAggr.getVersion() != agg.getVersion())
					throw new AggregationVersionException("Version number of aggreation does not match what is in the store.");
				
				existingAggr.setExchangeBlob(agg.getExchangeBlob());
				existingAggr.setVersion(agg.getVersion() + 1);
				agg = aggRepo.save(existingAggr);
			}
			else
			{
				// initial add... set the version number to 1
				agg.setVersion(agg.getVersion() + 1);
				agg = aggRepo.save(agg);
			}
			
			
        	// update the version on the exchange
        	exchange.setProperty(AGGREGATION_ENTITY_VERSON, agg.getVersion());
        }
        catch (Exception e) 
        {
        	// wrap exception in a runtime exception
            throw new RuntimeException("Error adding to repository aggregation with key " + key, e);
        }        
        
        // don't support getting the older version of the exchange... just return null
        // might support getting the older version in some later version
        return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Exchange get(CamelContext camelContext, String key) 
	{
		Exchange retVal = null;

		try
		{
			// get the aggregation
			final Optional<Aggregation> aggOpt = aggRepo.findById(key);
			if (!aggOpt.isPresent())
				return null;
			
			final Aggregation agg = aggOpt.get();
			
			// deserialized to an exchange object
			retVal = codec.unmarshallExchange(camelContext, new Buffer(agg.getExchangeBlob()));
			
			// set the version of the exchange for later consistency checking
			retVal.setProperty(AGGREGATION_ENTITY_VERSON, agg.getVersion());
		}
        catch (Exception e) 
        {
        	// wrap exception in a runtime exception
        	throw new RuntimeException("Error retrieving from repository aggregation with key " + key, e);
        }
		
		return retVal;
	}

	/**
	 * {@inheritDoc}
	 * 	This specific implementation throws a Runtime exception on DAO errors.  This implementation also checks for
	 * consistency/concurrency of the exchange.  If the attempted removal of an exchange does match the latest and greatest
	 * exchange version, then an AggregationVersionException is wrapped by the runtime error.  Routes using this
	 * repository should catch the AggregationVersionException and attempt to retry the exchange.  If exception handling
	 * and redelivery is configured correctly, Camel should automatically reload the exchange from the latest version in the 
	 * and attempt the aggregation and completion condition again.
	 */
	@Override
	@Transactional
	public void remove(CamelContext camelContext, String key, Exchange exchange) 
	{
        try 
        {
        	// get the version of the exchange
        	Integer currentEntityVersion = (Integer)exchange.getProperty(AGGREGATION_ENTITY_VERSON);
        	
        	// serialize the exchange to a byte array
            final byte[] blob = codec.marshallExchange(camelContext, exchange, true).getData();

            Aggregation agg = new Aggregation();
        	agg.setExchangeBlob(blob);
        	agg.setId(key);
        	agg.setVersion(currentEntityVersion == null ? 0 : currentEntityVersion);
        	
        	// removed the exchange from the currently working set and move it to completed set
        	// for later confirmation
        	final Optional<Aggregation> existingAggOpt = aggRepo.findById(key);
        	if (!existingAggOpt.isPresent())
        		throw new AggregationRepositoryException("Aggregation does not exist is store.");	
        	
        	
        	final Aggregation existingAgg = existingAggOpt.get();
			// check the version number for consistency
			if (existingAgg.getVersion() != agg.getVersion())
				throw new AggregationVersionException("Version number of aggreation does not match what is in the store.");		
			
			// remove
			aggRepo.delete(existingAgg);
			
        	
			// add to the completed repository
			final AggregationCompleted completed = new AggregationCompleted();
			
			completed.setExchangeBlob(existingAgg.getExchangeBlob());
			completed.setId(exchange.getExchangeId());
			completed.setVersion(1);
			
			aggCompRepo.save(completed);

        }
        catch (Exception e) 
        {
        	// wrap exception in a runtime exception
            throw new RuntimeException("Error removing from repository aggregation with key " + key, e);
        }        		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void confirm(CamelContext camelContext, String exchangeId) 
	{
        try 
        {
        	// confirm the aggregation and removed it from the repository
        	this.aggCompRepo.deleteById(exchangeId);
        }
        catch (EmptyResultDataAccessException e)
        {
        	/* no-op, this is fine */
        }
        catch (Exception e) 
        {
        	// wrap exception in a runtime exception
            throw new RuntimeException("Error confirming aggregation with key " + exchangeId, e);
        } 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getKeys() 
	{
        try 
        {
        	// get the list of keys
			List<String> keys = aggRepo.findAllKeys();
			
			// return an empty set if no keys are found
			if (keys == null || keys.isEmpty())
				return Collections.emptySet();
			
			// return an empty set if no keys are found
			return new LinkedHashSet<String>(keys);
        }
	    catch (Exception e) 
	    {
	    	// wrap exception in a runtime exception
	        throw new RuntimeException("Error retriving aggregation keys", e);
	    } 			
	}

	@Override
	public Set<String> scan(CamelContext camelContext) 
	{
        try 
        {		
        	// get the list of unconfirmed exchange keys
			final List<String> keys = aggCompRepo.findAllKeys();
			
			// return an empty set if no keys are found
			if (keys == null || keys.isEmpty())
				return Collections.emptySet();
			
			// return an empty set if no keys are found
			return new LinkedHashSet<String>(keys);
        }
	    catch (Exception e) 
	    {
	    	// wrap exception in a runtime exception
	        throw new RuntimeException("Error retriving aggregation completed keys", e);
	    } 				
	}

	/**
	 * {@inheritDoc}
	 * This specific implementation locks the recovered exchange for a period of time specified by the DAO.
	 * If the exchange is locked by the DAO, then null is returned.
	 */
	@Override
	public Exchange recover(CamelContext camelContext, String exchangeId) 
	{
		Exchange retVal = null;

		try
		{
			// recover the exchnage from the repository
			final Optional<AggregationCompleted> aggOpt = this.aggCompRepo.findById(exchangeId);
			
			// not found or is locked... return null
			if (!aggOpt.isPresent())
				return null;
			
			AggregationCompleted entity = aggOpt.get();
			
			if (entity.getRecoveryLockedUntilDtTm() != null && entity.getRecoveryLockedUntilDtTm().after(Calendar.getInstance(Locale.getDefault())))
				return null; 
			
			final Calendar newRecoveryLockTime = Calendar.getInstance(Locale.getDefault());
			newRecoveryLockTime.add(Calendar.SECOND, recoveredEntityLockInterval);
			entity.setRecoveryLockedUntilDtTm(newRecoveryLockTime);
			
			// persist the time new lock time and increment the update count
			aggCompRepo.save(entity);
			
			// deserialize exchange 
			retVal = codec.unmarshallExchange(camelContext, new Buffer(entity.getExchangeBlob()));
			
			// set the version number of the exchange
			retVal.setProperty(AGGREGATION_COMPLETE_ENTITY_VERSON, entity.getVersion());
		}
        catch (Exception e) 
        {
        	// wrap exception in a runtime exception
        	throw new RuntimeException("Error recovering exchange from repository with exchangeId " + exchangeId, e);
        }
		
		// reset the exception
		retVal.setException(null);
		
		return retVal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRecoveryInterval(long interval, TimeUnit timeUnit) 
	{
		this.recoveryInterval = timeUnit.toMillis(interval);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRecoveryInterval(long interval) 
	{
        this.recoveryInterval = interval;	
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getRecoveryIntervalInMillis() 
	{
		return recoveryInterval;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUseRecovery(boolean useRecovery) 
	{
		this.useRecovery = useRecovery;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isUseRecovery() 
	{
		return useRecovery;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDeadLetterUri(String deadLetterUri) 
	{
		this.deadLetterUri = deadLetterUri;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDeadLetterUri() 
	{
		return deadLetterUri;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMaximumRedeliveries(int maximumRedeliveries) 
	{
		this.maximumRedeliveries = maximumRedeliveries;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaximumRedeliveries() 
	{
		return maximumRedeliveries;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doStart() throws Exception 
	{
		/*
		 * Adapted from JdbcAggregationRepository
		 */
	
		if (aggCompRepo == null || aggRepo == null)
			throw new IllegalStateException("Aggregation respository sources cannot be null");
		
        // log number of existing exchanges
        int current = getKeys().size();
        int completed = scan(null).size();

        if (current > 0) 
        {
        	LOGGER.info("On startup there are " + current + " aggregate exchanges (not completed) in repository");
        } 
        else 
        {
        	LOGGER.info("On startup there are no existing aggregate exchanges (not completed) in repository");
        }
        
        if (completed > 0) 
        {
        	LOGGER.warn("On startup there are " + completed + " completed exchanges to be recovered in repository");
        } 
        else 
        {
        	LOGGER.info("On startup there are no completed exchanges to be recovered in repository");
        }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doStop() throws Exception 
	{
		/* no-op */
	}
	
	
}
