---
title: Deployment Guide
---

# Deployment Guide

The message monitor is a standalone service: it is never embedded inside the gateway or the security and trust agent. Other components feed it tracked messages and notifications over the network, and it independently decides when a delivery notification sequence has gone unanswered long enough to warrant a failure notification back to the original sender's edge client.

There are two deployment models for the monitor. **The cloud native model is the only model supported going forward** and is where all new work happens; the legacy model is documented for HISPs still running an older release and planning a migration.

## Cloud Native Model (recommended)

The monitor runs as a self-contained Spring Boot micro-service — `direct-msg-monitor-sboot-<version>.jar` — alongside the other reference implementation micro-services, communicating over REST and a message broker (RabbitMQ by default). It is a plain Java process with no application server to install, so it deploys unchanged onto bare metal, Docker, Kubernetes, Cloud Foundry, or a managed runtime such as Google Cloud Run.

This is the model used by the [Cloud Native HISP deployment](/docs/direct-project-stock/cloud-native-deployment) of the BareMetal Assembly Project, which shows the monitor in the context of the full HISP topology.

* [Cloud Native Deployment](cloud-native-deployment) — how to deploy and wire up the monitor micro-service.

## Legacy Model

The monitor is packaged as `direct-msg-monitor-web-<version>.war` and deployed into a J2EE servlet container — in practice Apache Tomcat, alongside the configuration service and XD in the pre-9.0 BareMetal assembly.

The war artifact was **last released at version 8.0.0 and is not published for 9.0 or later**. The reference implementation retired Tomcat as of the 9.0 release. If you are running this model, treat it as a migration source rather than a deployment target.

* [Legacy Deployment](legacy-deployment) — configuration and operation of the war deployment, plus what changes when migrating.

## Shared References

The following apply to both models:

* [Configuration Reference](dep-and-config) — every configuration property the monitor reads, grouped by concern.
* [Deployment Considerations](dep-considerations) — scaling, high availability, and shared state.
