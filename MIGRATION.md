# Migration Guide

This document outlines breaking changes and migration steps between versions of `java-coap`.

- [Upgrading from 6.x to 7.0](#upgrading-from-6x-to-70)

---

## Upgrading from 6.x to 7.0

### TL;DR

- **Module removed:** The deprecated `lwm2m` module is no longer published.
- **Unified package prefix:** All packages under `com.mbed.coap.*` and `org.opencoap.*` are unified under the `opencoap.*` namespace.
- **Domain package reorganization:** Classes are organized into domain-focused packages (`opencoap.core`, `opencoap.codec`, `opencoap.endpoint`, `opencoap.filter`, `opencoap.routing`, `opencoap.observe`, `opencoap.transport`, `opencoap.linkformat`, `opencoap.util`).
- **Fluently modify requests & responses:** Deprecated `CoapRequest.with*` methods are removed in favor of `modify()`. Added `modify()` to `CoapResponse` and `SeparateResponse`.
- **Query options:** `query(String)` that split on `&` is removed. Use `queries(String...)`, `queries(List<String>)`, or `query(name, value)`.
- **Class renames:**
  - `MediaTypes` &rarr; `ContentFormat`
  - `SignallingHeaderOptions` &rarr; `SignalingHeaderOptions`
  - `Method.iPATCH` &rarr; `Method.IPATCH`
  - `CoapRequestEntityIncomplete` &rarr; `CoapRequestEntityIncompleteException`
  - `CoapRequestEntityTooLarge` &rarr; `CoapRequestEntityTooLargeException`
  - `CoapBlockTooLargeEntityException` &rarr; `CoapBlockEntityTooLargeException`
  - `MessageIdSupplierImpl` &rarr; `SequentialMessageIdSupplier`
  - `CapabilitiesStorageImpl` &rarr; `HashMapCapabilitiesStorage`
- **Content-Format constants & uint16 typing:** `ContentFormat` constants dropped the `CT_` prefix (e.g. `APPLICATION_JSON`), fixed typos (`APPLICATION_COSE_*`, `APPLICATION_LINK_FORMAT`, `APPLICATION_OCTET_STREAM`), and content formats are now typed as `int`/`Integer` (RFC 7252 uint16) instead of `short`/`Short`.

---

### 1. Module Changes

The deprecated `lwm2m` module has been removed. If your project depended on it, remove the dependency. Note that standard IANA LwM2M content formats (`ContentFormat.APPLICATION_LWM2M_TLV` and `ContentFormat.APPLICATION_LWM2M_JSON`, formerly `MediaTypes.CT_APPLICATION_LWM2M_*`) remain available in `coap-core`.

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
+import opencoap.core.ContentFormat;
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
| `com.mbed.coap.packet` | `opencoap.core` | `CoapRequest`, `CoapResponse`, `SeparateResponse`, `Code`, `Method`, `MessageType`, `ContentFormat`, `BlockOption`, `BlockSize`, `HeaderOptions`, `BasicHeaderOptions`, `SignalingOptions`, `Opaque` |
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

---

### 3. Deprecated Methods & Builders

#### Modifying CoapRequest

The `with*` mutation methods on `CoapRequest` have been removed in favor of `modify()`.

```diff
-CoapRequest updated = request.withPayload(newPayload);
+CoapRequest updated = request.modify().payload(newPayload).build();
```

```diff
-CoapRequest updated = request.withToken(newToken);
+CoapRequest updated = request.modify().token(newToken).build();
```

```diff
-CoapRequest updated = request.withAddress(peerAddress);
+CoapRequest updated = request.modify().address(peerAddress).build();
```

```diff
-CoapRequest updated = request.withOptions(opt -> opt.etag(etag));
+CoapRequest updated = request.modify().options(opt -> opt.etag(etag)).build();
```

#### Separate Responses

The 4-argument `SeparateResponse` constructor and `toSeparate(..., TransportContext)` overloads have been removed. Set transport context on the response prior to converting to separate response, or use `modify()`:

```diff
-SeparateResponse sep = response.toSeparate(token, peerAddress, transContext);
+SeparateResponse sep = response.withContext(transContext).toSeparate(token, peerAddress);
```

```diff
-SeparateResponse sep = new SeparateResponse(response, token, peerAddress, transContext);
+SeparateResponse sep = new SeparateResponse(response.withContext(transContext), token, peerAddress);
```

You can now also fluently modify existing `CoapResponse` and `SeparateResponse` instances:

```java
CoapResponse updatedResponse = response.modify().payload("new payload").build();
SeparateResponse updatedSeparate = separateResponse.modify().payload("new payload").build();
```

---

### 4. URI Query Options

The ambiguous `query(String)` method that split query strings on `&` has been removed. Use `queries(String...)`, `queries(List<String>)`, or `query(String name, String value)`.

#### On CoapRequest.Builder and CoapOptionsBuilder

```diff
-requestBuilder.query("key1=val1&key2=val2");
+requestBuilder.queries("key1=val1", "key2=val2");
```

```diff
-requestBuilder.query("filter=active");
+requestBuilder.query("filter", "active");
```

#### On BasicHeaderOptions

```diff
-String query = options.getUriQuery();
+List<String> queryList = options.getUriQueryList();
+// Or, to get percent-encoded URI query string according to RFC 7252:
+String encodedQuery = options.getUriQueryEncoded();
```

```diff
-options.setUriQuery("key1=val1&key2=val2");
+options.setUriQueryList(List.of("key1=val1", "key2=val2"));
+// Or add individual query items:
+options.addUriQuery("key1=val1");
+options.addUriQuery("key2=val2");
```

---

### 5. Renamed Classes and Members

#### ContentFormat (RFC 7252 naming)

Renamed from `MediaTypes` to `ContentFormat` to match the CoAP specification (RFC 7252) and describe the integer content format registry rather than MIME media types.

In addition, all `ContentFormat` constants dropped the redundant `CT_` prefix (e.g. `APPLICATION_JSON` rather than `CT_APPLICATION_JSON`), and long-standing defects in constant names were corrected:

```diff
-import com.mbed.coap.packet.MediaTypes;
+import opencoap.core.ContentFormat;

-options.accept(MediaTypes.CT_APPLICATION_JSON);
+options.accept(ContentFormat.APPLICATION_JSON);
```

##### Corrected Constant Names

| Old Constant (6.x `MediaTypes`) | New Constant (7.0 `ContentFormat`) | Notes |
|---|---|---|
| `CT_APPLICATION_LINK__FORMAT` | `APPLICATION_LINK_FORMAT` | Removed double underscore |
| `CT_APPLICATION_OCTET__STREAM` | `APPLICATION_OCTET_STREAM` | Removed double underscore |
| `CT_APPLICATION_CODE_ENCRYPT0` | `APPLICATION_COSE_ENCRYPT0` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_MAC0` | `APPLICATION_COSE_MAC0` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_SIGN1` | `APPLICATION_COSE_SIGN1` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_ENCRYPT` | `APPLICATION_COSE_ENCRYPT` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_MAC` | `APPLICATION_COSE_MAC` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_SIGN` | `APPLICATION_COSE_SIGN` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_KEY` | `APPLICATION_COSE_KEY` | Corrected "CODE" typo to "COSE" (RFC 8152) |
| `CT_APPLICATION_CODE_KEY_SET` | `APPLICATION_COSE_KEY_SET` | Corrected "CODE" typo to "COSE" (RFC 8152) |

#### Signaling Header Options (RFC 8323 spelling)

Spelling corrected from `Signalling` to `Signaling` to match RFC 8323 and the existing `SignalingOptions` class.

```diff
-import com.mbed.coap.packet.SignallingHeaderOptions;
+import opencoap.core.SignalingHeaderOptions;

-SignallingHeaderOptions options = new SignallingHeaderOptions(Code.C701_CSM);
-options.putSignallingOptions(signalingOptions);
-SignalingOptions sig = options.toSignallingOptions(Code.C701_CSM);
+SignalingHeaderOptions options = new SignalingHeaderOptions(Code.C701_CSM);
+options.putSignalingOptions(signalingOptions);
+SignalingOptions sig = options.toSignalingOptions(Code.C701_CSM);
```

#### Exception Class Renames

Exceptions now uniformly end with the `Exception` suffix:

```diff
-catch (CoapRequestEntityTooLarge e) {
+catch (CoapRequestEntityTooLargeException e) {
     // ...
 }
```

```diff
-catch (CoapRequestEntityIncomplete e) {
+catch (CoapRequestEntityIncompleteException e) {
     // ...
 }
```

```diff
-catch (CoapBlockTooLargeEntityException e) {
+catch (CoapBlockEntityTooLargeException e) {
     // ...
 }
```

#### Implementation Class Renames

Implementation classes have been renamed to describe their concrete structure rather than using an `Impl` suffix:

```diff
-MessageIdSupplier supplier = new MessageIdSupplierImpl();
+MessageIdSupplier supplier = new SequentialMessageIdSupplier();
```

```diff
-CapabilitiesStorage storage = new CapabilitiesStorageImpl();
+CapabilitiesStorage storage = new HashMapCapabilitiesStorage();
```

#### Enum Constant Rename

```diff
-Method method = Method.iPATCH;
+Method method = Method.IPATCH;
```

#### CLI Transport Codec

```diff
-opencoap.cli.transport.CoapSerializer
+opencoap.cli.transport.CoapPacketCodec
```

---

### 6. Content-Format and Option Types (uint16)

#### uint16 Content-Format Representation

RFC 7252 defines Content-Format identifiers as unsigned 16-bit integers (`uint16`, range `0..65535`). Previously, `java-coap` represented them as signed `short` / `Short` (`-32768..32767`). As a consequence, any content format &ge; 32768 (the upper half of the IANA registry, including the 65000+ experimental range) was truncated to a negative number during parsing and serialized incorrectly.

Content format is now uniformly represented as `int` / `Integer`:

- **Constants:** `ContentFormat` constants are `public static final int`.
- **Options & Builders:** `BasicHeaderOptions`, `CoapOptionsBuilder`, `CoapRequest.Builder`, and `CoapResponse.Builder` accept and return `int` / `Integer` for `contentFormat` and `accept`.
- **Range validation:** `BasicHeaderOptions.setContentFormat(Integer)` and `setAccept(Integer)` validate that values fall within `0..65535` (`0xFFFF`), throwing `IllegalArgumentException` otherwise.
- **Removed overload trap:** The `BasicHeaderOptions.setAccept(short)` overload has been removed to eliminate ambiguity with `setAccept(Integer)`.
- **LinkFormat:** `LinkFormat.getContentType()` and `setContentType(Integer)` now use `Integer` instead of `Short`.
- **Utility methods:** `ContentFormat.contentFormatToString(Integer)` and `ContentFormat.parseContentFormat(String)` use `Integer`.

```diff
-short cf = MediaTypes.CT_APPLICATION_JSON;
+int cf = ContentFormat.APPLICATION_JSON;
```

```diff
-CoapResponse response = CoapResponse.ok("{}", (short) 50);
+CoapResponse response = CoapResponse.ok("{}", ContentFormat.APPLICATION_JSON);
```

```diff
-Short ct = linkFormat.getContentType();
+Integer ct = linkFormat.getContentType();
```

#### Option Numbers and Constant Type Corrections

Other option-related constants and fields that could not represent their full domains have also been corrected:

- **Option number constants:** `BasicHeaderOptions` option constants (`IF_MATCH`, `URI_HOST`, `ETAG`, `IF_NON_MATCH`, `URI_PORT`, `LOCATION_PATH`, `URI_PATH`, `CONTENT_FORMAT`, `MAX_AGE`, `URI_QUERY`, `ACCEPT`, `LOCATION_QUERY`, `PROXY_URI`, `PROXY_SCHEME`, `SIZE1`) changed from `byte` to `int` (CoAP option numbers are unsigned integers).
- **Default Max-Age:** `BasicHeaderOptions.DEFAULT_MAX_AGE` changed from `short` (`60`) to `long` (`60L`), matching the `Long maxAge` field.
- **Max retransmit:** `CoapConstants.MAX_RETRANSMIT` changed from `Short` to primitive `int` (`4`).
