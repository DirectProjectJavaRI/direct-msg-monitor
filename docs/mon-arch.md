---
title: Message Monitor Architecture
---

# Message Monitor Architecture

As stated in the overview, several patterns exist to address common integration problems that arise with enterprise application architecture. These design patterns have been grouped in a category called enterprise integration patterns, or EIP. The first definitive EIP [book](http://www.eaipatterns.com/) describes over 60 integration patterns and problem sets addressed by each. [Apache Camel](http://camel.apache.org/) is an open source set of Java libraries that implements all 60+ patterns, plus a vast number of ever increasing converters and end point connectors.

## Monitoring Aggregator

The message monitor is built on the aggregator [pattern](http://camel.apache.org/aggregator.html). In its raw application, an aggregator combines multiple messages into one single message. As messages flow into an aggregator, they are continually combined into one message using using three distinct rule based components:

* Correlator: Determines which messages should be combined together.
* Aggregator: Determines how the correlated messages are combined into a single message. This may include complex processing and/or translation and conversion.
* Complete Condition: Determines when the combined message is complete and can be moved on to the next step in a "route".

The message monitor naturally builds on each of these concepts to track messages for delivery notification completion. The correlator component determines what messages should be combined together. For notification message, all messages are correlated back to the original (sometime called 'parent' in the Direct RI) message id. The aggregator component creates a simple collection of correlated messages and their attributes and places this collection into the aggregated message body. It also keeps track of when the notification process started for timely delivery purposes as an attribute on the Camel message envelope. The completion condition determines if all notification messages have been received for the original message. In Camel, release conditions may have an al timeout attribute that indicates that the aggregated message should be completed with whatever has been aggregated up to that point.

At its heart, Camel is a highly configurable rules and processing engine where the process is defined by a "route". Routes are defined by utilizing a Camel domain-specific language (DSL). Routes are generally written directly in Java code (or some other JVM language like Scala and Groovy), or can be defined using Spring XML configuration files. For message monitoring, the aggregator is defined using the Spring based DSL and declaring each of the aggregator components in the Spring configuration XML file. The aggregator is just part of a Camel route which include an entry endpoint (from) and an optional exit endpoint (to). The following is a example Spring XML based Camel route utilizing the monitoring aggregator components.

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

Notice the beans and methods in the route definition. These beans are declared in the same Spring application context that the route is defined/created in.

Also notice the 'choice' element. The components below the completionTimeout element are where the aggregeted message is routed after the completion or timeout condition is met. For the purposes of the route above, the aggregated message is routed to a Camel 'choice' component that determines if a failure notification should be generated. If all notification messages have been received, then processing stops as noted by the 'stop' element. Otherwise, the message is routed to the dsnMessageProcessor to create the failure notification, and then the dsnSender.

## Monitor Client

So how do messages actually get into the monitoring service? The direct-msg-monitor-model library defines the monitoring data structures, and the direct-msg-monitor-client library defines a client interface along with a message parser that converts MIME messages into a structure of monitored attributes. The client library also provides an implementation of that interface backed by RESTful calls to the monitoring service. This implies that the monitoring service is never deployed as part of the gateway application — it is always a separate, centrally deployed service.

As messages flow through the messaging gateway, the gateway uses the client to feed messages and notifications into the monitoring service. The client auto-configures itself as soon as `direct.msgmonitor.service.url` is set on the calling service; leave it unset and the caller performs no tracking at all.

The monitor server also ships a Spring Cloud Stream ingest path, which reads `Tx` events from a message broker destination and feeds them into the same Camel route as the REST endpoint. It is not the default — it is activated by the `streams` Spring profile. See [Cloud Native Deployment](cloud-native-deployment) for how both paths are wired.
