---
title: Cloud Native Deployment
---

# Cloud Native Deployment

In the cloud native model the message monitor is a discrete Spring Boot micro-service — a single fat jar, `direct-msg-monitor-sboot-<version>.jar` — that runs as an ordinary Java process. There is no application server to install and no war to drop anywhere. Because it is just a process, the same artifact deploys unchanged to bare metal, Docker, Kubernetes, Cloud Foundry, or a managed runtime such as Google Cloud Run.

This page covers deploying the monitor on its own. For the monitor in the context of a complete HISP — the other micro-services, the network zones they sit in, and the broker destinations that connect them — see the [Cloud Native HISP Deployment Model](/docs/direct-project-stock/cloud-native-deployment) in the BareMetal Assembly Project.

## How the Monitor Is Wired In

The monitor has one inbound path (tracked messages and notifications arriving from the rest of the HISP) and one outbound path (failure notifications it generates itself). The two paths use different transports by default.

```
   STA ────┐
           │  POST /txs  (REST — default ingest)
   James ──┤
           │                                  ┌───────────────────┐
           └─────────────────────────────────►│                   │
                                              │  Message Monitor  │
   broker destination:                        │    (aggregator)   │
   direct-tx-monitoring ────────────────────► │                   │
   (only with the "streams" profile)          └─────────┬─────────┘
                                                        │
                                       generated DSN on timeout
                                                        │
                                 binding: direct-smtp-gateway-message-out-0
                                 destination: direct-smtp-mq-gateway
                                                        │
                                                        ▼
                                              STA ingest queue
```

**Inbound — tracked messages.** The default ingest is the REST API. The security and trust agent and James call `POST /txs` on the monitor for every message and notification they process, and call `POST /txs/suppressNotification` before releasing a notification to an edge client. Both callers use the `direct-msg-monitor-client` library, which is auto-configured as soon as `direct.msgmonitor.service.url` is set on the calling service — see [Wiring Other Services to the Monitor](#wiring-other-services-to-the-monitor).

A broker-based ingest also ships in the jar: activating the `streams` Spring profile registers a consumer on the `direct-tx-monitoring` destination that feeds `Tx` events into the same Camel route. It is **off by default** — without the profile the binding is inert and REST is the only ingest.

**Outbound — generated failure notifications.** When an aggregation times out without all required notifications, the monitor generates a DSN and, by default, publishes it back onto the broker rather than sending it over SMTP itself. The message goes out on the `direct-smtp-gateway-message-out-0` binding, whose destination defaults to `direct-smtp-mq-gateway` — the security and trust agent's ingest queue — so a monitor-generated DSN re-enters the pipeline through the same entry point as any other message. This is controlled by `direct.msgmonitor.dsnSender.useStreamsSender`, which defaults to `true`. The alternative, `useSMTPGatewaySender`, sends the DSN directly to an SMTP gateway instead. The two are mutually exclusive; setting both to `true` fails startup with an `IllegalStateException`.

## Prerequisites

| Requirement | Notes |
| :--- | :--- |
| Java runtime | Matching the JDK baseline of the release you are deploying. |
| Message broker | RabbitMQ by default. Required unless you switch the DSN sender to `useSMTPGatewaySender` **and** leave the `streams` profile off. |
| Database | See [Database and Schema](#database-and-schema). The default is an ephemeral in-memory database — not suitable for anything beyond a smoke test. |

If you do not already have a broker, the quickest option is a container:

```
docker run -d --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

This runs with the default guest/guest credentials the reference implementation services expect out of the box.

## Obtain the Binary

Download the fat jar from Maven Central and place it in its own directory:

| Service | Jar File | Suggested Directory |
| :--- | :--- | :--- |
| Message Monitor | [direct-msg-monitor-sboot-9.0.0.jar](https://repo.maven.apache.org/maven2/org/nhind/direct-msg-monitor-sboot/9.0.0/direct-msg-monitor-sboot-9.0.0.jar) | `message-monitor` |

## Run It

The monitor is a standard Spring Boot application:

```
java -jar direct-msg-monitor-sboot-9.0.0.jar
```

For anything other than a quick trial, wrap the launch in a start/stop script rather than backgrounding `java -jar` by hand. The BareMetal Assembly Project publishes generic `service.sh` and `service.ps1` templates plus a `logback.xml` that work for every micro-service including this one — see [Launch Microservices](/docs/direct-project-stock/cloud-native-deployment#launch-microservices). Copy the template into the `message-monitor` directory and substitute the jar name for the `<binary>` placeholder.

The service listens on HTTP port `8081` by default. Override with `server.port`.

### Verify

The monitor exposes a plain HTML health page that requires no credentials:

```
http://<server>:8081/health
```

Spring Boot Actuator is also on the classpath, so `/actuator/health` is available as well.

## Configuration

Every setting is an ordinary Spring Boot property, so all the usual [externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) mechanisms apply: an `application.yml` alongside the jar, environment variables, command-line arguments, a Spring Cloud Config server, or a mounted config tree. The full list of properties the monitor reads is in the [Configuration Reference](dep-and-config); this section covers only what a real deployment must change.

The shipped defaults are tuned for a single-node trial on a developer machine. At minimum, change the datasource and the broker connection.

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://db.internal:5432/msgmonitor
    username: msgmonitor
    password: <your password>

  rabbitmq:
    host: rabbit.internal
    port: 5672
    username: msgmonitor
    password: <your password>

  security:
    user:
      name: admin
      password: '{bcrypt}<your bcrypt hash>'

direct:
  msgmonitor:
    condition:
      generalConditionTimeout: 3600000
      reliableConditionTimeout: 3600000
    dsnSender:
      useStreamsSender: true
```

### Database and Schema

The shipped `application.yml` ships with its datasource block commented out. With no datasource configured, Spring Boot falls back to an embedded in-memory database, which means **every aggregation in flight and the entire duplicate-notification history are lost on restart**. Configure a real datasource before doing anything beyond a smoke test. MySQL, PostgreSQL, Derby, and H2 drivers are all bundled in the fat jar.

You do not need to create the tables yourself. The monitor builds its own `EntityManagerFactory` with DDL generation enabled, so its four tables are created on first startup against whatever database you point it at:

| Table | Holds |
| :--- | :--- |
| `msgmonaggregation` | Active aggregations — the correlated message sets still waiting on notifications. |
| `msgmonaggregationcomp` | Completed aggregation bookkeeping. |
| `receivednotification` | Notifications already delivered to an edge client, for duplicate suppression. |
| `pendingnotification` | DSNs the monitor generated itself, correlated back to the original message id, so a late-arriving real notification can still be reconciled. |

All you need to provision is an empty database or schema and a user with DDL privileges on it.

### Broker Bindings

The stream destinations are conventional Spring Cloud Stream bindings and can be renamed if your broker topology differs from the reference implementation's:

| Binding | Default Destination | Purpose |
| :--- | :--- | :--- |
| `direct-smtp-gateway-message-out-0` | `direct-smtp-mq-gateway` | Where monitor-generated DSNs are published (STA ingest). |
| `directTxMonitoring-in-0` | `direct-tx-monitoring` | Broker ingest for `Tx` events. Only bound when the `streams` profile is active. |

Redirecting the outbound DSNs to a different queue, for example:

```yaml
spring:
  cloud:
    stream:
      bindings:
        direct-smtp-gateway-message-out-0:
          destination: my-sta-ingest
```

Note that the shipped configuration disables the RabbitMQ health indicator (`management.health.rabbit.enabled: false`), so broker connectivity does not contribute to the service's health status. Enable it if you would rather have a broker outage mark the instance unhealthy.

### Wiring Other Services to the Monitor

The monitor does not discover its callers; the callers are pointed at it. On the security and trust agent and on James, set:

| Name | Description |
| :--- | :--- |
| `direct.msgmonitor.service.url` | Base URL of the monitor, for example `http://message-monitor.internal:8081/`. Setting this property is what activates the monitor client auto-configuration — leave it unset and the calling service does no tracking at all. |
| `direct.webservices.security.basic.user.name` | Basic auth user name presented on calls to the monitor. |
| `direct.webservices.security.basic.user.password` | Basic auth password presented on calls to the monitor. |
| `direct.webservices.connect.timeout` | Connect timeout in milliseconds. Default `5000`. |
| `direct.webservices.response.timeout` | Response timeout in milliseconds. Default `10000`. |

::: warning Security
The `SecurityWebFilterChain` shipped with the service disables CSRF and declares no authorization rules, so the `/txs` API is not protected by the service itself — the `spring.security.user` credentials in the shipped configuration are not enforced on it. Keep the monitor's HTTP port inside the internal HISP network, and supply your own filter chain (or front the service with a gateway that authenticates) if the port is reachable from anywhere else.
:::

## Containers and Kubernetes

Nothing about the monitor is container-specific — build an image on any Java base image with the fat jar as its entrypoint. A few settings are worth knowing for a Kubernetes deployment:

* **Probes.** Point `livenessProbe` and `readinessProbe` at `/actuator/health/liveness` and `/actuator/health/readiness`. Spring Boot enables those two health groups automatically when it detects that it is running in Kubernetes; set `management.endpoint.health.probes.enabled: true` to expose them on any other platform.
* **Secrets.** Rather than baking credentials into the image, mount them and let Spring read them as a config tree: `SPRING_CONFIG_IMPORT=optional:configtree:/etc/secrets/`. Each file under that directory becomes a property named after its path.
* **Direct memory.** The broker client allocates off-heap buffers, so cap direct memory explicitly on a container with a memory limit, for example `JAVA_TOOL_OPTIONS=-XX:MaxDirectMemorySize=256m`.
* **Replicas.** The aggregator holds state in a shared database rather than in memory, so more than one replica is supported — but read [Deployment Considerations](dep-considerations) first, particularly on the recovery lock and the dead-letter destination.

A reference `Deployment` and `Service` manifest is maintained in the `k8s` directory of the `direct-msg-monitor-sboot` repository.
