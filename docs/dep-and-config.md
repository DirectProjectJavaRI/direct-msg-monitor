---
title: Configuration Reference
---

# Configuration Reference

Every setting the message monitor reads, grouped by concern. Properties marked **Cloud native only** have no effect in the [legacy war deployment](legacy-deployment), which has no message broker.

Where the property is supplied depends on the deployment model. In the [cloud native model](cloud-native-deployment) these are ordinary Spring Boot properties, so any of the standard [externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) sources will do — an `application.yml` beside the jar, environment variables, command-line arguments, a Spring Cloud Config server, or a mounted config tree. In the legacy model they go in `bootstrap.properties` under the deployed application's `WEB-INF/classes` directory.

Note that a few properties use a bare `monitor.` prefix rather than `direct.msgmonitor.`. These are called out where they appear; they are not typos.

## Runtime and Transport

| Name | Description | Default |
| :--- | :--- | :--- |
| `server.port` | HTTP port the service listens on. | `8081` |
| `spring.datasource.*` | Database connection settings. See the Spring Boot [data properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.data) for the full set. **The shipped cloud native configuration leaves this unset**, which falls back to an ephemeral in-memory database — configure it before any real deployment. | *(unset)* |
| `spring.rabbitmq.*` | Broker connection settings. See the Spring Boot [integration properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.integration) for the full set. **Cloud native only.** | host: `localhost`<br>port: `5672`<br>username: `guest`<br>password: `guest` |
| `spring.security.user.name` | User name in the shipped security configuration. | `admin` |
| `spring.security.user.password` | Password in the shipped security configuration, stored as a bcrypt hash. The shipped hash decodes to `d1r3ct;`. | `d1r3ct;` |
| `management.health.rabbit.enabled` | Whether broker connectivity contributes to the service's health status. Disabled in the shipped configuration. **Cloud native only.** | `false` |
| `spring.profiles.active` | Set to `streams` to additionally consume tracked messages from the broker rather than REST alone. **Cloud native only.** | *(unset)* |

## Stream Bindings

**Cloud native only.** Conventional Spring Cloud Stream bindings — override the destinations if your broker topology differs from the reference implementation's.

| Name | Description | Default |
| :--- | :--- | :--- |
| `spring.cloud.stream.bindings.direct-smtp-gateway-message-out-0.destination` | Destination that monitor-generated DSNs are published to. Defaults to the security and trust agent's ingest queue, so a generated DSN re-enters the pipeline through the same entry point as any other message. | `direct-smtp-mq-gateway` |
| `spring.cloud.stream.bindings.directTxMonitoring-in-0.destination` | Destination that tracked `Tx` events are consumed from. Only bound when the `streams` profile is active. | `direct-tx-monitoring` |
| `spring.cloud.stream.bindings.directTxMonitoring-in-0.consumer.*` | Consumer concurrency and retry behavior for the ingest binding. | concurrency: `10`<br>maxAttempts: `10`<br>backOffInitialInterval: `15000`<br>backOffMaxInterval: `120000` |

## Aggregation Timeout

How long a set of correlated messages stays open waiting for the notifications that would complete it. When the timeout expires without completion, the monitor generates a failure notification. See [Completion and Timeout Condition Components](comp-and-timeout) for what the two conditions actually evaluate.

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.condition.generalConditionTimeout` | Time in milliseconds a general (non-reliable) aggregation is held before timing out. | `3600000` (1 hour) |
| `direct.msgmonitor.condition.reliableConditionTimeout` | Time in milliseconds a timely-and-reliable aggregation is held before timing out. | `3600000` (1 hour) |

## Failure Notification Delivery

How a generated DSN leaves the monitor. `useStreamsSender` and `useSMTPGatewaySender` are mutually exclusive — setting both to `true` fails startup with an `IllegalStateException`.

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.dsnSender.useStreamsSender` | Publish generated DSNs to the message broker instead of sending them over SMTP. **Cloud native only.** | `true` |
| `direct.msgmonitor.dsnSender.useSMTPGatewaySender` | Send generated DSNs directly over SMTP to `gatewayURL`. The only option in the legacy model. | `false` |
| `direct.msgmonitor.dsnSender.gatewayURL` | SMTP URL of the mail gateway that delivers or relays the DSN. Only used when `useSMTPGatewaySender` is `true`. | `smtp://localhost:25` |

## Failure Notification Content

Content of the DSN the monitor generates on timeout. The generator supports HTML, so these values may contain markup. Two substitution tags are available: `%original_sender_tag%` in the header, and `%headers_tag%` in the footer.

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.dsnGenerator.postmasterName` | Postmaster account name used as the `From` address of the DSN. Prepended to the original sender's domain. | `postmaster` |
| `direct.msgmonitor.dsnGenerator.mtaName` | Name of the agent reported as having created the DSN. | `DirectProject Message Monitor` |
| `direct.msgmonitor.dsnGenerator.subjectPrefix` | Prefix added to the DSN subject line. | `Not Delivered:` |
| `direct.msgmonitor.dsnGenerator.failedRecipientsTitle` | Text introducing the list of recipients for whom no timely notification arrived. | `We have not received a delivery notification in 1 hour for the following recipient(s) because the receiving system may be down or configured incorrectly:` |
| `direct.msgmonitor.dsnGenerator.errorMessageTitle` | Title at the top of the human-readable section. | *(empty)* |
| `direct.msgmonitor.dsnGenerator.defaultErrorMessage` | Human-readable explanation of why the message failed. | *(a paragraph advising the sender to confirm recipient addresses)* |
| `direct.msgmonitor.dsnGenerator.header` | Introduction at the top of the human-readable section. Supports `%original_sender_tag%`. | `%original_sender_tag%,<br/>` |
| `direct.msgmonitor.dsnGenerator.footer` | Footer at the bottom of the human-readable section, generally troubleshooting information. Supports `%headers_tag%`. | `<b><u>Troubleshooting Information</u></b><br/><br/>%headers_tag%` |

## Aggregator Persistence, Recovery, and Retry

Camel holds active aggregations in a database-backed repository so that they survive a restart. After an aggregation completes or times out it moves on to the DSN generator and sender; if that step fails, these settings govern the retry and eventual give-up behavior. See [Deployment Considerations](dep-considerations) for how the recovery lock behaves across multiple instances.

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.recovery.retryInterval` | Milliseconds between recovery attempts after a failure. | `30000` |
| `direct.msgmonitor.recovery.maxRetryAttemps` | Maximum redelivery attempts before the aggregation is dead-lettered. (The property name is spelled as shown in the code, missing the second `t` in "Attempts".) | `12` |
| `direct.msgmonitor.recovery.deadLetterUri` | Destination for aggregations that exhaust their retries. By default a file, whose contents are a `toString()` representation of the collection of `Tx` objects in the aggregation. | `file:recovery/directMonitorDeadLetter` |
| `monitor.aggregatorRepository.recoveryLockInterval` | Seconds an in-recovery aggregation stays locked to one instance before another may retry it. Note the `monitor.` prefix. | `120` |

## State Retention

How long the monitor keeps its duplicate-suppression and pending-DSN records before purging them. The purge itself runs on a Camel timer every six hours — see [Notification Duplication Checking](dup-checking).

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.dupStateDAO.retensionTime` | Time in **days** that delivered-notification state is kept before being purged. | `7` |
| `monitor.dupStateDAO.retensionTime` | A second name for the same setting, injected directly onto the state manager. Note the `monitor.` prefix. Set this to the same value as the property above so the change takes effect regardless of injection order. | `7` |
| `direct.msgmonitor.pendingStateDAO.retensionTime` | Time in **hours** that a monitor-generated DSN stays on file as pending, awaiting a late-arriving real notification to reconcile against, before it is purged. | `24` |

## Camel Route

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.route.start.endpointuri` | Internal Camel endpoint that tracked messages are fed into. Change this only if you are supplying your own route definition — see [Extending and Writing Custom Components](custom-components). | `direct:start` |

## Client-Side Settings

These are set on the **calling** services — the security and trust agent and James — not on the monitor itself. They are what points those services at the monitor; without `direct.msgmonitor.service.url` the client auto-configuration does not activate and the caller performs no tracking at all.

| Name | Description | Default |
| :--- | :--- | :--- |
| `direct.msgmonitor.service.url` | Base URL of the monitor service. | *(unset — tracking disabled)* |
| `direct.webservices.security.basic.user.name` | Basic auth user name presented on calls to the monitor. | *(empty)* |
| `direct.webservices.security.basic.user.password` | Basic auth password presented on calls to the monitor. | *(empty)* |
| `direct.webservices.connect.timeout` | Connect timeout in milliseconds. | `5000` |
| `direct.webservices.response.timeout` | Response timeout in milliseconds. | `10000` |
| `direct.webservices.retry.backoff.initialBackoffInterval` | Initial retry backoff in milliseconds. | `100` |
| `direct.webservices.retry.backoff.multiplier` | Backoff multiplier between retries. | `3` |
| `direct.webservices.retry.backoff.maxInterval` | Maximum retry backoff in milliseconds. | `20000` |
