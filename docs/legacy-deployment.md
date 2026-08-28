---
title: Legacy Deployment
---

# Legacy Deployment

::: warning End of life
The war artifact was last published at **version 8.0.0**. There is no 9.0 or later release of `direct-msg-monitor-web`, because the reference implementation retired Apache Tomcat as of the 9.0 release. This page exists for HISPs still running an 8.0.0-or-earlier deployment and planning a migration. New deployments should use the [Cloud Native Deployment](cloud-native-deployment).
:::

In the legacy model the monitor is packaged as a war, `direct-msg-monitor-web-<version>.war`, and deployed into a J2EE servlet container. In the pre-9.0 BareMetal assembly that container is the bundled Apache Tomcat instance, which also hosts the configuration service, the configuration UI, and XD. The wider legacy HISP topology is described in the [Legacy HISP Deployment Model](/docs/direct-project-stock/legacy-deployment) of the BareMetal Assembly Project.

## Deployment

Drop the war into the container's deployment directory. The application context is whatever the container assigns — typically the war file name — so the monitor's URLs are determined by the deployer rather than by the application.

To confirm the deployment came up, request the health page:

```
http://<server>:<port>/<application context>/health
```

A running service returns a simple HTML page confirming it is up.

## Configuration

Configuration lives in a properties file named `bootstrap.properties`, found under the deployed application's `WEB-INF/classes` directory. Edit it in place and restart the container. Unlike the cloud native model there is no externalized configuration source, no config server, and no environment-variable overriding.

The property names themselves are largely the same as in the cloud native model — the aggregation timeouts, the DSN generator content, and the recovery and retry settings are all read under the same `direct.msgmonitor.*` keys. The [Configuration Reference](dep-and-config) marks which properties are cloud-native only.

Two differences matter in practice:

* **DSN delivery is always over SMTP.** The broker-based DSN sender (`direct.msgmonitor.dsnSender.useStreamsSender`) does not apply. Generated failure notifications are sent to the SMTP gateway named by `direct.msgmonitor.dsnSender.gatewayURL`.
* **Tracked messages arrive only over REST.** The gateway and the security and trust agent post to the monitor's `/txs` endpoint through the monitor client library. There is no broker ingest.

The default database is a local Derby instance:

```
spring.datasource.url=jdbc:derby:msgmonitor;create=true
spring.datasource.username=nhind
spring.datasource.password=nhind
```

Derby is file based and allows only one process to open the database at a time, so it rules out running more than one instance. See [Deployment Considerations](dep-considerations) for moving to a shared RDBMS.

## Migrating to the Cloud Native Model

The monitor is one of the easier components to move, because its behavior, its property names, and its database schema are unchanged. In outline:

1. **Keep the database.** The schema is the same, and the entity mappings generate it automatically. Point the new service at the existing database and in-flight aggregations carry over.
2. **Swap the artifact.** Replace the war and its container with `direct-msg-monitor-sboot-<version>.jar` run as a plain Java process.
3. **Move `bootstrap.properties` into `application.yml`.** The `direct.msgmonitor.*` keys transfer as-is; `spring.datasource.*` becomes the YAML equivalent.
4. **Decide how DSNs leave.** The cloud native default publishes generated DSNs to the broker rather than sending them over SMTP. To preserve the legacy behavior exactly, set `direct.msgmonitor.dsnSender.useSMTPGatewaySender: true` and `direct.msgmonitor.dsnSender.useStreamsSender: false`, keeping your existing `gatewayURL`. Otherwise leave the streams sender on and let the security and trust agent relay them.
5. **Repoint the callers.** Update `direct.msgmonitor.service.url` on the security and trust agent and James to the new host and port — the monitor defaults to `8081` rather than the container's port and application context.

See [Cloud Native Deployment](cloud-native-deployment) for the full target-state instructions.
