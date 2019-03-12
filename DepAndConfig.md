# Deployment and Configuration

The monitoring service is typically deployed as a RESTful web service using two differnt models:

* In a J2EE container such as Apache Tomcat. The service is packaged in a war file named direct-msg-monitor-web-<version>.war. The application context is determined by the deployer and web container. To validate deployment, the service contains a simple health check URI that can be accessed by simply pointing a browser to the following URI.  If the monitor service is running, the service will return a simple HTML page indicating that the service is running.

```
http://<server:port>/<application context>/health
```

* As a standalone SpringBoot microservice application.  The service is package in a SpringBoot jar named direct-msg-monitor-sboot-<version>.jar.  The application is can be run by running the following command.  

```
java -jar direct-msg-monitor-sboot-<version>.jar
```

Using the SpringBoot option, it also possible to deploy to platforms such as CloudFoundry.  The source code repository contains a sample manifest.yml for deploying to a CloudFoundry instance.

## Service Configuration

The service defines Spring bean Java classes as part of its default configuration.  The default bean configuration is generally fine for most deployments leaving other deployment type decisions such as database connections to be configured in a properties file.  Almost all of the configurable parameters are externalized into a properties file named bootstrap.properties under the <app home>/WEB-INF/classes directory if deployed into Tomcat. If deployed as a SpringBoot application, there are several options for configuration such as using a SpringCloud configuration server or using a local properties file.  More information on externalizing SpringBoot configuration can be found [here](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).

The next sections break down the different configuration parameters in the bootstrap.properties file.

**Aggregation Timeout**

The following properties tune the configuration for aggregation timeout:

* direct.msgmonitor.condition.generalConditionTimeout - The time in milliseconds the general messages are held in the aggregator before timing out. The default is 3600000 (1 hour).
* direct.msgmonitor.condition.reliableConditionTimeout - The time in milliseconds the general messages are held in the aggregator before timing out. The default is 3600000 (1 hour).

**Message Failure Generator**

The following properties control the content of different parts of the DNSMessageGenerator. The generator and support HTML content for rich presentation formats.

* direct.msgmonitor.dsnGenerator.postmasterName - The postmaster account name used as the from attribute for DSN messages. The postmaster name will be pre-appended to the domain name of the original sender. Default value is 'postmaster'.
* direct.msgmonitor.dsnGenerator.mtaName - The name of the agent creating the DSN message.  Default value is 'DirectProject Message Monitor'.
* direct.msgmonitor.dsnGenerator.subjectPrefix - A string prefixed to the subject of the DSN message.  Default value is 'Not Delivered'.
* direct.msgmonitor.dsnGenerator.failedRecipientsTitle - Title that goes above the list of failed recipients in the human readable section of the DSN message.
* direct.msgmonitor.dsnGenerator.errorMessageTitle - Title that goes at the top of the human readable section of the DSN message.
* direct.msgmonitor.dsnGenerator.defaultErrorMessage - A human readable description of why the message failed to be delivered.
* direct.msgmonitor.dsnGenerator.header - A message header that appears at the top of the human readable section of the DSN message. This generally used as the message introduction.
* direct.msgmonitor.dsnGenerator.footer - A footer at the bottom of the human readable section of the DSN message. This is generally used to provide troubleshooting information.
* direct.msgmonitor.route.start.endpointuri - Internal Camel endpoint used to process monitoring messages.  Default value is 'direct:start'.
* direct.msgmonitor.dsnSender.gatewayURL - SMTP URL of the mail gateway that will either deliver or relay DSN message.

**Aggregator Persistence, Recovery, and Retry**

The default Camel implementation of the aggregator holds active message in memory. If the service were to crash, the active messages would be lost. To prevent message loss, Camel provides a configurable mechanism for persisting and recovering active messages. After the aggregator completion condition is met (or timesout), Camel delivers the aggregated message to the next step in the route. As you can see in our Spring DSL, messages are moved onto the DSN generator. If for some reason the messages cannot be delivered to the gatewayURL, it is necessary to retry sending the messages to the gateway. The properties file contains configuration properties for controlling all of these parameters.

* direct.msgmonitor.recovery.retryInterval - If the DSN message cannot be sent to the gateway URL, this parameter in the interval in milliseconds between each retry attempt.  Default value is '30000' (5 minutes)
* monitor.aggregatorRepository.recoveryLockInterval - Interval in seconds of how long a service instance locks an aggregation row.  Default value is '120' (2 minutes).
* direct.msgmonitor.recovery.maxRetryAttemps - The maximum retry attempts to send the DSN message to the gateway URL.  Default value is '12'
* direct.msgmonitor.recovery.deadLetterUri - If the DSN message exhausts all retry attempts, the parameter indicates a dead letter URL that aggregated message will be sent to. By default, this a file (file:recovery/directMonitorDeadLetter). The contents of the file is a .toString() representation of the collection of Tx objects in the aggregated message.
