# Overview

The message monitoring module is aggregating tracked message in a HISP and determining if failure notification messages need to be sent to a message originator based on specific conditions. The default implementation of this module implements a generic condition algorithm for messages that do not conform to the [implementation guide](http://wiki.directproject.org/w/images/a/a1/Implementation_Guide_for_Delivery_Notification_in_Direct_v1.0.pdf) for delivery notification of the Direct Project. It also implements the monitoring conditions as specified by the the reliable and timely messaging implementation guidance of the Direct Project.

## Guides

This document describes the Direct Project message monitor service for timely and reliable delivery, architecture and developers guide, and deployment models and options.

* [Overview](Overview) - This section describes the purpose of the monitoring service and how it fits with the rest of the timely and reliable delivery implementation.
* [Development Guide](DevGuide) - This section describes the architecture and components of the monitoring service and how to develop your own custom components.
* [Deployment Guide](DepGuide) - This section describes the deployment model of the messaging service, and how it can be customized with stock properties or using your own custom components.