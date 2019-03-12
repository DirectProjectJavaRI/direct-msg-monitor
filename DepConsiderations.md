# Deployment Considerations

The default deployment makes some assumptions about the deployment model. It assumes a single instance of the monitoring service running a local derby database. For a scalable and highly available service, configuration of a separated RDBMS is necessary. By default, the service supports PostgreSQL, MySQL, and Oracle.  The following sections outline some considerations for moving to scalable and highly available deployment model.

## Aggregator State

By nature, aggregators are stateful. The default Camel aggregator uses an in memory model to hold state which means it is not readily scalable across multiple instances of the service. It may be necessary to write a custom aggregator state module that persists and obtains state from a centralized database. Camel provides interfaces to enable this type state persistence, but still requires custom code to be written the properly support distributed state.

As of direct-msg-monitor-1.1, a distributable and stateful implementation of the aggregator repository is available. This implementation (org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository) is aware of concurrency and consistency issues related to updating aggregation exchanges across multiple threads, JVMs, and even nodes across an cluster. In the event of a concurrency issue, the default applicationContext.xml configuration uses the onException camel construct to reload the aggregation exchange from its latest state and attempt the aggregation and completion condition logic again. For failed exchange recovery, the repository implements a time based lock that ensures only one service instance attempts to recover the failed exchange. The default lock time is two minutes, but can be tweaked using the 'monitor.aggregatorRepository.recoveryLockInterval' property.

## Duplication State Store

The default deployment uses the Derby embedded database. This database is file based and only allows a single process to access the database at any time. A true RDBMS such as MySQL, Oracle, or PostgreSQL is necessary for running multiple instances. Fortunately, this only requires the bootstrap.properties file to be modified with the proper driver class and JDBC URL (or whatever externalized configuration you are using if running as a SpringBoot application).

The following is the default configuration used for connecting to the local derby database:

```
spring.datasource.url=jdbc:derby:msgmonitor;create=true
spring.datasource.username=nhind
spring.datasource.password=nhind
```

You can make configuration changes using property setting found [here](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html) in the SpringBoot documentation.

## Retry Dead Letter URL

The default deployment writes out string representation of the aggregated message is the DSN message cannot be sent to the gateway URL. A system that requires reporting or more sophisticated reply may need to change the representation of the messages or the URL of the dead letter queue.

