---
title: Deployment Considerations
---

# Deployment Considerations

The shipped defaults describe a single instance on a developer machine, not a service you would put in front of real traffic. The sections below cover what has to change to run the monitor scaled out and highly available. They apply to both deployment models, though the cloud native model is the one built to scale horizontally.

## Database

Every piece of state the monitor holds — active aggregations, delivered-notification history, pending generated DSNs — lives in its database. That makes the database the single thing you must get right before anything else.

The cloud native jar ships with **no datasource configured**, which leaves Spring Boot to fall back to an embedded in-memory database. Everything in flight is lost on restart. The legacy war defaults to a local Derby instance, which is file based and admits only one process at a time, so it rules out running a second instance at all.

Configure a real shared RDBMS. MySQL and PostgreSQL drivers are bundled in the cloud native jar; the entity mappings generate the schema automatically, so you only need to provision an empty database and a user with DDL privileges on it. See [Database and Schema](cloud-native-deployment#database-and-schema).

## Aggregator State

Aggregators are stateful by nature, and Camel's default aggregator holds that state in memory — which would make the service impossible to scale past one instance.

The monitor does not use it. Since `direct-msg-monitor-1.1` it ships `org.nhindirect.monitor.aggregator.repository.ConcurrentJPAAggregationRepository`, an aggregation repository that keeps state in the shared database and is aware of the concurrency and consistency problems of updating an aggregation exchange from multiple threads, JVMs, and cluster nodes at once. When two instances collide on the same exchange, the route's `onException` handler reloads the exchange from its latest persisted state and re-runs the aggregation and completion logic.

For failed exchange recovery the repository takes a time-based lock so that only one instance attempts to recover a given exchange. The lock lasts two minutes by default; tune it with `monitor.aggregatorRepository.recoveryLockInterval`. Set it long enough to cover a recovery attempt's realistic worst case — too short and two instances can both take a run at the same exchange.

Once the database is shared, running multiple instances is a matter of starting more of them.

## Retry Dead Letter Destination

When a generated DSN cannot be handed off — to the broker or to the SMTP gateway, depending on which sender is configured — the aggregation is retried and eventually dead-lettered. The default destination is a local file containing a `toString()` representation of the collection of `Tx` objects in the aggregation.

Two things to weigh here. The representation is a debugging aid, not a machine-readable record, so if you want alerting or reporting on dropped notifications you will need a different format. And a local file is per-instance state: on a scaled-out or containerized deployment each replica writes its own, and on an ephemeral filesystem those disappear with the container. Point `direct.msgmonitor.recovery.deadLetterUri` at something durable and shared — Camel accepts any endpoint URI, so a queue is a reasonable choice.

## Broker

**Cloud native only.** The monitor's outbound path depends on the message broker being available, and the shipped configuration disables the RabbitMQ health indicator, so a broker outage does not show up in the service's health status. Monitor the broker separately, or enable `management.health.rabbit.enabled` and let broker connectivity gate the instance's health — but be deliberate about that choice, since it means a broker blip can take every instance out of rotation at once.

If you activate the `streams` profile for broker-based ingest, the consumer's concurrency and retry settings become part of your capacity planning. See [Stream Bindings](dep-and-config#stream-bindings).

## Timeout Tuning

Both aggregation timeouts default to one hour. That value is also written into the default text of `direct.msgmonitor.dsnGenerator.failedRecipientsTitle` ("We have not received a delivery notification in 1 hour…"), so if you change a timeout, change that message to match — otherwise your senders get a failure notification that misstates how long the monitor actually waited.
