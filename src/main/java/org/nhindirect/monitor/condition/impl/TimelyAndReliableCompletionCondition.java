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

package org.nhindirect.monitor.condition.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.common.mail.dsn.DSNStandard;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.monitor.repository.ReceivedNotificationRepository;
import org.nhindirect.monitor.repository.RepositoryBiz;

import lombok.extern.slf4j.Slf4j;


/**
 * A completion condition that implements the timely and reliable messaging for Direct specification.
 * @author Greg Meyer
 * @since 1.0
 */
@Slf4j
public class TimelyAndReliableCompletionCondition extends AbstractCompletionCondition
{	
	protected ReceivedNotificationRepository recRepo;
	
	public void setReceivedNotificationRepository(ReceivedNotificationRepository recRepo)
	{
		this.recRepo = recRepo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public Collection<String> getIncompleteRecipients(Collection<Tx> txs)
	{
		if (txs == null || txs.size() == 0)
			return Collections.emptyList();
		
		final Tx originalMessage = getMessageToTrackInternal(txs);
		if (originalMessage == null)
			return Collections.emptyList();
		
		final TxDetail originalRecipDetail = originalMessage.getDetail(TxDetailType.RECIPIENTS.getType());
		if (originalRecipDetail == null)
			return Collections.emptyList();
		
		// add the original recipient list to a map of recipients to status
		final Map<String, RecipientResponseStatus> recipStatuses = new HashMap<String, RecipientResponseStatus>();
		for (String recip : originalRecipDetail.getDetailValue().split(","))
			recipStatuses.put(recip.trim(), new RecipientResponseStatus(recip.trim()));
		
		
		for (Tx tx : txs)
		{
			
			final TxDetail finalRecipDetail = tx.getDetail(TxDetailType.FINAL_RECIPIENTS);
			
			if (finalRecipDetail != null)
			{
				switch (tx.getMsgType())
				{
				   case MDN:
				   {
					   // an MDN is sent per original message recipient, so we should only be able
					   // to extract one original recipient from this message
					   String normalizedFinalRecip = normalizeFinalRecip(finalRecipDetail.getDetailValue().trim());
					   final RecipientResponseStatus recipStatus = recipStatuses.get(normalizedFinalRecip);
					   if (recipStatus == null) {
							log.warn(
									"The final recipient '{}' on the MDN with message ID '{}' does not correspond to one of the recipients of the original message with message ID '{}'",
									normalizedFinalRecip, tx.getDetail(TxDetailType.MSG_ID).getDetailValue(),
									originalMessage.getDetail(TxDetailType.MSG_ID).getDetailValue());
					   }
					   final TxDetail dispDetail = tx.getDetail(TxDetailType.DISPOSITION);
					   if (dispDetail != null && recipStatus != null)
					   {	
						   final String dispValue = dispDetail.getDetailValue();
						   
						   // check if this is an MDN processed message
						   if (dispValue.contains(MDNStandard.Disposition_Processed))
							   recipStatus.addReceivedStatus(RecipientResponseStatus.MDNProcessReceived);
						   // check if this is an MDN dispatched message
						   else if (dispValue.contains(MDNStandard.Disposition_Dispatched))
						   {
							   // check for the reliable and timely option
							   final TxDetail mdnOptionDetail = tx.getDetail(TxDetailType.DISPOSITION_OPTIONS);
							   if (mdnOptionDetail != null && mdnOptionDetail.getDetailValue().toLowerCase().
									   contains(MDNStandard.DispositionOption_TimelyAndReliable.toLowerCase()))
								   recipStatus.addReceivedStatus(RecipientResponseStatus.MDNDispatchedReceived);
						   }
						   // check if this is an MDN failed message
						   else if (dispValue.contains(MDNStandard.Disposition_Denied) || dispValue.contains(MDNStandard.Disposition_Error))
							   recipStatus.addReceivedStatus(RecipientResponseStatus.MDNFailedReceived);
					   }
					   break;
				   }
				   case DSN:
				   {
					   // get DSN action
					   final TxDetail actionDetail = tx.getDetail(TxDetailType.DSN_ACTION);
					   if (actionDetail != null)
					   {	
						   final String actionValue = actionDetail.getDetailValue();
						   if (actionValue.contains(DSNStandard.DSNAction.FAILED.toString()))
						   {
							   // there can be multiple final recipients in a DSN message
							   for (String finalRecip : finalRecipDetail.getDetailValue().split(","))
							   {
								   String normalizedFinalRecip = normalizeFinalRecip(finalRecip.trim());
								   final RecipientResponseStatus recipStatus = recipStatuses.get(normalizedFinalRecip);
								   if (recipStatus != null)
									   recipStatus.addReceivedStatus(RecipientResponseStatus.DSNFailedReceived);
								   else
										log.warn(
												"The final recipient '{}' on the DSN with message ID '{}' does not correspond to one of the recipients of the original message with message ID '{}'",
												normalizedFinalRecip,
												tx.getDetail(TxDetailType.MSG_ID).getDetailValue(),
												originalMessage.getDetail(TxDetailType.MSG_ID).getDetailValue());
							   }
						   }
					   }   
				   }
				}
			}
		}
		
		final Collection<String> retVal = new ArrayList<String>();
		
		// iterate through the recipient list and make sure each one is complete
		for (RecipientResponseStatus status : recipStatuses.values())
		{
			if (!((status.isMDNDispatchedReceived() && status.isMDNProcessedReceived()) || // did we receive both processed and dispatched
					(status.isMDNFailedReceived() || status.isDSNFailedReceived())))  // did we receive an MDN or DSN failure
			{
				// if at least one of the conditions above is not true (that being why the entire statement is 
				// preceded with a !, then that recipient is not complete
				// the completion condition of the original message is not considered complete until all recipients
				// have been accounted for
				retVal.add(status.getRecipient());
			}
			else
			{
				TxDetail detail = originalMessage.getDetail(TxDetailType.MSG_ID);
				if (detail != null)
					RepositoryBiz.addMessageToDuplicateStore(detail.getDetailValue(), status.getRecipient(), recRepo);
			}
		}
		
		return retVal;
	}
	

	
	private static class RecipientResponseStatus
	{
		public static final short MDNProcessReceived = 0x0001;
		public static final short MDNDispatchedReceived = 0x0002;
		public static final short MDNFailedReceived = 0x0004;		
		public static final short DSNFailedReceived = 0x0008;
		
		protected int statusesReceived = 0;
		protected final String recipient;		
		
		RecipientResponseStatus(String recipient)
		{
			this.recipient = recipient;
		}
		
		public void addReceivedStatus(short status)
		{
			statusesReceived |= status;
		}
		
		public boolean isMDNProcessedReceived()
		{
			return ((statusesReceived & MDNProcessReceived) > 0);
		}
		
		public boolean isMDNDispatchedReceived()
		{
			return ((statusesReceived & MDNDispatchedReceived) > 0);
		}
		
		public boolean isMDNFailedReceived()
		{
			return ((statusesReceived & MDNFailedReceived) > 0);
		}
		
		public boolean isDSNFailedReceived()
		{
			return ((statusesReceived & DSNFailedReceived) > 0);
		}
		
		public String getRecipient()
		{
			return recipient;
		}
	}
}
