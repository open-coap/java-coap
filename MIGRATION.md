# Migration Guide

This document outlines breaking changes and migration steps between versions of `java-coap`.

- [Upgrading from 6.x to 7.0](#upgrading-from-6x-to-70)

---

## Upgrading from 6.x to 7.0

### TL;DR

- **Module removed:** The deprecated `lwm2m` module is no longer published.
- **Unified package prefix:** All packages under `com.mbed.coap.*` and `org.opencoap.*` are unified under the `opencoap.*` namespace.
- **Domain package reorganization:** Classes are organized into domain-focused packages (`opencoap.core`, `opencoap.codec`, `opencoap.endpoint`, `opencoap.filter`, `opencoap.routing`, `opencoap.observe`, `opencoap.transport`, `opencoap.linkformat`, `opencoap.util`).

---

### 1. Module Changes

The deprecated `lwm2m` module has been removed. If your project depended on it, remove the dependency. Note that standard IANA LwM2M content formats (`MediaTypes.CT_APPLICATION_LWM2M_TLV` and `MediaTypes.CT_APPLICATION_LWM2M_JSON`) remain available in `coap-core`.

#### Gradle

```diff
 dependencies {
   implementation("io.github.open-coap:coap-core:VERSION")
-  implementation("io.github.open-coap:lwm2m:VERSION")
 }
```

#### Maven

```diff
-<dependency>
-  <groupId>io.github.open-coap</groupId>
-  <artifactId>lwm2m</artifactId>
-  <version>VERSION</version>
-</dependency>
```

---

### 2. Package Structure & Imports

All classes have been migrated from legacy prefixes (`com.mbed.coap.*`, `org.opencoap.coap.*`, `org.opencoap.transport.*`) to `opencoap.*` and restructured by domain.

#### Common Imports Diff

```diff
-import com.mbed.coap.client.CoapClient;
-import com.mbed.coap.packet.BlockSize;
-import com.mbed.coap.packet.Code;
-import com.mbed.coap.packet.CoapRequest;
-import com.mbed.coap.packet.CoapResponse;
-import com.mbed.coap.packet.MediaTypes;
-import com.mbed.coap.packet.Opaque;
-import com.mbed.coap.server.CoapServer;
-import com.mbed.coap.server.RouterService;
-import com.mbed.coap.server.filter.TokenGeneratorFilter;
-import com.mbed.coap.server.observe.HashMapObservationsStore;
-import com.mbed.coap.transport.TransportContext;
-import com.mbed.coap.transport.udp.DatagramSocketTransport;
-import com.mbed.coap.utils.Filter;
-import com.mbed.coap.utils.Service;
+import opencoap.core.BlockSize;
+import opencoap.core.Code;
+import opencoap.core.CoapRequest;
+import opencoap.core.CoapResponse;
+import opencoap.core.Filter;
+import opencoap.core.MediaTypes;
+import opencoap.core.Opaque;
+import opencoap.core.Service;
+import opencoap.core.TransportContext;
+import opencoap.endpoint.CoapClient;
+import opencoap.endpoint.CoapServer;
+import opencoap.filter.TokenGeneratorFilter;
+import opencoap.observe.HashMapObservationsStore;
+import opencoap.routing.RouterService;
+import opencoap.transport.DatagramSocketTransport;
```

#### Package Mapping Reference

| Old Package (6.x) | New Package (7.0) | Primary Contents |
|---|---|---|
| `com.mbed.coap` | `opencoap.core` | `CoapConstants` |
| `com.mbed.coap.packet` | `opencoap.core` | `CoapRequest`, `CoapResponse`, `SeparateResponse`, `Code`, `Method`, `MessageType`, `MediaTypes`, `BlockOption`, `BlockSize`, `HeaderOptions`, `BasicHeaderOptions`, `SignalingOptions`, `Opaque` |
| `com.mbed.coap.packet` | `opencoap.codec` | `CoapPacket`, `CoapSerializer`, `RawOption`, `DataConvertingUtility`, `CoapTcpPacketSerializer`, `CoapTcpPacketConverter` |
| `com.mbed.coap.exception` | `opencoap.core` | `CoapException`, `CoapCodeException`, `CoapTimeoutException`, `CoapBlockException` |
| `com.mbed.coap.exception` | `opencoap.codec` | `CoapMessageFormatException` |
| `com.mbed.coap.exception` | `opencoap.filter` | `TooManyRequestsForEndpointException` |
| `com.mbed.coap.client` | `opencoap.endpoint` | `CoapClient` |
| `com.mbed.coap.client` | `opencoap.linkformat` | `RegistrationManager` |
| `com.mbed.coap.server` | `opencoap.endpoint` | `CoapServer`, `CoapServerBuilder`, `CoapServerGroup`, `TcpCoapServer`, `CoapRequestId` |
| `com.mbed.coap.server` | `opencoap.routing` | `RouterService` |
| `com.mbed.coap.server` | `opencoap.observe` | `ObservationHandler`, `ObserveRequestFilter`, `NotificationValidator` |
| `com.mbed.coap.server.messaging` | `opencoap.endpoint` | `CoapDispatcher`, `CoapTcpDispatcher`, `Capabilities`, `MessageIdSupplier`, `RequestTagSupplier` |
| `com.mbed.coap.server.messaging` | `opencoap.endpoint.pipeline` | `ExchangeFilter`, `PiggybackedExchangeFilter`, `TcpExchangeFilter`, `DuplicateDetector`, `CriticalOptionVerifier`, `RescueFilter`, `RetransmissionFilter`, `PayloadSizeVerifier` |
| `com.mbed.coap.server.block` | `opencoap.endpoint.pipeline` | `BlockWiseIncomingFilter`, `BlockWiseOutgoingFilter`, `BlockWiseNotificationFilter`, `BlockWiseTransfer` |
| `com.mbed.coap.server.filter` | `opencoap.filter` | `CongestionControlFilter`, `EchoFilter`, `EtagGeneratorFilter`, `EtagValidatorFilter`, `MaxAllowedPayloadFilter`, `RequestLoggerFilter`, `ResponseTimeoutFilter`, `TokenGeneratorFilter` |
| `com.mbed.coap.server.observe` | `opencoap.observe` | `HashMapObservationsStore`, `NotificationsReceiver`, `ObservationsStore`, `ObserversManager` |
| `com.mbed.coap.transmission` | `opencoap.endpoint` | `RetransmissionBackOff` |
| `com.mbed.coap.transport` | `opencoap.core` | `TransportContext` |
| `com.mbed.coap.transport` | `opencoap.transport` | `CoapTransport`, `BlockingCoapTransport`, `CoapTcpTransport`, `CoapTcpListener`, `LoggingCoapTransport` |
| `com.mbed.coap.transport.udp` | `opencoap.transport` | `DatagramSocketTransport` |
| `com.mbed.coap.transport.javassl` | `opencoap.transport` | `SocketClientTransport`, `SSLSocketClientTransport` |
| `com.mbed.coap.transport.stdio` | `opencoap.transport` | `StreamBlockingTransport`, `OpensslProcessTransport` |
| `com.mbed.coap.linkformat` | `opencoap.linkformat` | `LinkFormat`, `LinkFormatBuilder`, `PToken` |
| `com.mbed.coap.utils` | `opencoap.core` | `Service`, `Filter` |
| `com.mbed.coap.utils` | `opencoap.util` | `FutureHelpers`, `ExecutorHelpers`, `Timer`, `Validations` |
| `org.opencoap.coap.netty` | `opencoap.transport` | `NettyCoapTransport`, `CoapCodec`, `NettyUtils` |
| `org.opencoap.transport.mbedtls` | `opencoap.transport` | `MbedtlsCoapTransport`, `DtlsSessionSuspensionService`, `DtlsTransportContext` |
| `org.opencoap.coap.metrics.micrometer` | `opencoap.filter` | `MicrometerMetricsFilter` |
