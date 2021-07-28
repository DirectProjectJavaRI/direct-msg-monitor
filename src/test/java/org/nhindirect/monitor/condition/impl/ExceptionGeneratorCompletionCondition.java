package org.nhindirect.monitor.condition.impl;

import java.util.Collection;

import org.nhindirect.common.tx.model.Tx;

public class ExceptionGeneratorCompletionCondition extends AbstractCompletionCondition
{

	@Override
	public Collection<String> getIncompleteRecipients(Collection<Tx> txs) 
	{
		throw new RuntimeException("ExceptionGeneratorCompletionCondition genererated exception");
	}

}
