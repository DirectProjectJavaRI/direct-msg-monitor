# Message Failure Generation

The value proposition of the message monitor is to generate failure notifications if notifications are not received in a timely manner. If a timeout occurs, the monitor must generate an appropriate failure notification that will delivere to the original message's edge client.

## After The Aggregator

An aggregator may complete if one of two conditions is met: the aggregation is complete as defined by the completion condition or a timeout occurred. When the aggregator is complete, the aggregated messages is moved on to the next component in the Camel route. These components need to determine if the aggregator completed due to a timeout or if the completion condition was actually met, and then take appropriate action. In Camel, there are different construct to determine this. The default configuration of the monitoring service uses a 'choice' component. Let's take another look at the route define in Spring DSL.

```
<routes xmlns="http://camel.apache.org/schema/spring">
   <route id="direct-reliable">
      <from uri="direct:start"/>
      <onException>
           <exception>org.nhindirect.monitor.repository.AggregationVersionException</exception>
           <redeliveryPolicy maximumRedeliveries="-1" 
                redeliveryDelay="200" collisionAvoidanceFactor="0.15"/>
      </onException>         
      <aggregate strategyRef="aggregationStrategy" ignoreInvalidCorrelationKeys="true"
                aggregationRepositoryRef="directMonitoringRepo">
        <correlationExpression>
            <ref>msgIdCorrelator</ref>
        </correlationExpression>
        <completionPredicate>
          <method bean="aggregationStrategy" method="isAggregationComplete"/>
        </completionPredicate>     
        <completionTimeout>
          <method bean="aggregationStrategy" method="getAggregationTime"/>
        </completionTimeout>  
        <choice>
           <when>
              <method bean="aggregationStrategy" method="isAggregationComplete"/>
              <stop/>
           </when>
           <otherwise>
              <bean ref="dsnMessageProcessor"/>
              <bean ref="duplicationStateManager" method ="addNotificationForMonitorGeneratedDSN"/>    
              <bean ref="dsnSender"/>  
           </otherwise>
        </choice>      
      </aggregate>
    </route>
    
    <!--  Simple timer to purge exchanges in the duplication data base.
          This can replaced more sophisticated quartz configuration using 
          the Camel Quartz component and cron expressions.  Default configuration
          purges the table once every 6 hours.
     -->    
    <route id="duplicate-store-purge">
      <from uri="timer://purgeTimer?period=6h"/>
      <bean ref="duplicationStateManager" method="purge"/>
    </route> 

</routes>  
```

The choice calls the isAggregationComplete complete method on the provided aggregationStrategy bean to determine if all notification messages have been received. There are other ways to do this, but using a method construct allows for greater flexibility. The default aggregationStretegy is an instance of the BasicTxAggregator class. The isAggregationComplete method of this class simply calls its configured completion condition (by default, its configured to be an instance of the VariableCompletionCondition class) to determine if the completion condition has been met. If completion condition is met, then there is no need to generate a failure message, and the route is completed using the 'stop' component. Otherwise (as denoted by the 'otherwise' element of the choice component), the failure notification generator and sender is called.

## Failure Notification Generation

If the completion condition is not met and a timeout has occurred, then a failure notification message is created by utilizing the DSNMessageGenerator class. This class is instantiated and configured using Spring XML and provided as a bean reference to the Camel route. The generator creates and DSN failure message and includes a list of all recipients that did not provide all of the required notifications. The generates DNS message is then moved on to DSNMailSender.

The sender is another component instantiated and configured via Spring. It is responsible for sending the DSN message to an appropriate endpoint for delivery to the edge client.