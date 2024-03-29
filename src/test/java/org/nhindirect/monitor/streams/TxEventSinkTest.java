package org.nhindirect.monitor.streams;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.monitor.SpringBaseTest;
import org.nhindirect.monitor.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("streams")
public class TxEventSinkTest extends SpringBaseTest 
{	
	@Autowired 
	private ObjectMapper mapper;
	
	@Autowired 
	private ProducerTemplate producerTemplate;
	
	@Autowired
	private TxEventSink sink;
	
	@Test
	public void testSendTxToSink() throws Exception
	{
		final ProducerTemplate spyProducer = spy(producerTemplate);
	
		sink.setProducerTemplate(spyProducer);
		
		// send original message
		final String originalMessageId = UUID.randomUUID().toString();	
		final Tx originalMessage = TestUtils.makeReliableMessage(TxMessageType.IMF, originalMessageId, "", "gm2552@cerner.com", "gm2552@direct.securehealthemail.com", "", "", "");
		
		final String marshedTx = mapper.writeValueAsString(originalMessage);

		sink.directTxMonitoring().accept(marshedTx);
		
		verify(spyProducer, times(1)).sendBody(any());
	}
}
