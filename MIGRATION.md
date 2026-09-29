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
- **Header options unified:** `BasicHeaderOptions` and `HeaderOptions` are merged into a single `CoapOptions` class.
- **Option wire framing moved:** `CoapOptions.serialize(OutputStream)` and `deserialize(InputStream, int)` moved to `CoapSerializer.serializeOptions(CoapOptions, OutputStream)` and `CoapSerializer.deserializeOptions(CoapOptions, InputStream, int)`.
- **Service renamed to Handler:** `Service<REQ, RES>` is now `Handler<REQ, RES>`; `RouterService` is now `RoutingHandler`.
- **Filter hierarchy inverted:** `Filter<REQ, RES>` is now the type-preserving filter (formerly `Filter.SimpleFilter`). The general 4-type-parameter form is now `MappingFilter<REQ, RES, IN_REQ, IN_RES>`. `Filter.UnaryFilter` is removed.
- **TransportContext renamed to MessageAttributes:** `TransportContext` is now `MessageAttributes`, its nested `Key<T>` is the top-level `AttributeKey<T>`, and the accessors are `getAttributes()` / `getAttribute(key)` / `withAttributes(...)` / `addAttribute(...)`.
- **Class renames:**
  - `Service` &rarr; `Handler`
  - `RouterService` &rarr; `RoutingHandler`
  - `DtlsSessionSuspensionService` &rarr; `DtlsSessionSuspensionHandler`
  - `Filter<REQ, RES, IN_REQ, IN_RES>` &rarr; `MappingFilter<REQ, RES, IN_REQ, IN_RES>`
  - `Filter.SimpleFilter<REQ, RES>` &rarr; `Filter<REQ, RES>`
  - `TransportContext` &rarr; `MessageAttributes`
  - `TransportContext.Key<T>` &rarr; `AttributeKey<T>`
  - `DtlsTransportContext` &rarr; `DtlsAttributes`
  - `MediaTypes` &rarr; `ContentFormat`
  - `BasicHeaderOptions` / `HeaderOptions` &rarr; `CoapOptions`
  - `SignallingHeaderOptions` &rarr; `SignalingCoapOptions`
  - `Method.iPATCH` &rarr; `Method.IPATCH`
  - `CoapRequestEntityIncomplete` &rarr; `CoapRequestEntityIncompleteException`
  - `CoapRequestEntityTooLarge` &rarr; `CoapRequestEntityTooLargeException`
  - `CoapBlockTooLargeEntityException` &rarr; `CoapBlockEntityTooLargeException`
  - `MessageIdSupplierImpl` &rarr; `SequentialMessageIdSupplier`
  - `CapabilitiesStorageImpl` &rarr; `HashMapCapabilitiesStorage`
  - `CoapRequestId` &rarr; `CoapMessageKey`
  - `Timer` &rarr; `Scheduler` (`Timer.toTimer` &rarr; `Scheduler.toScheduler`)
  - `CoapServerBuilderForTcp` &rarr; `TcpCoapServerBuilder`
  - `PayloadSizeVerifier` &rarr; `MaxMessageSizeFilter`
  - `Validations.assume` &rarr; `Validations.check`
  - `Method.valueOf(int)` / `MessageType.valueOf(int)` / `Code.valueOf(int)` &rarr; `fromCode(int)`
  - `LinkFormatBuilder` &rarr; `LinkFormatParser`
- **Method renames:** leftover, misspelled and inconsistent method names are corrected, e.g. `CoapPacket.headers()` &rarr; `options()`, `CoapServer.clientService()` &rarr; `outboundHandler()`, `CoapServerBuilder.midSupplier(...)` &rarr; `messageIdSupplier(...)`. `LinkFormatBuilder` is renamed to `LinkFormatParser`. See [section 11](#11-renamed-methods).
- **Reduced visibility:** a few internal helpers that were public in 6.x are now package-private, and `BlockingCoapTransport.sendPacket0` is now `protected`. See [section 10](#10-reduced-visibility).
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
+import opencoap.core.Handler;
+import opencoap.core.MessageAttributes;
+import opencoap.core.Opaque;
+import opencoap.endpoint.CoapClient;
+import opencoap.endpoint.CoapServer;
+import opencoap.filter.TokenGeneratorFilter;
+import opencoap.observe.HashMapObservationsStore;
+import opencoap.routing.RoutingHandler;
+import opencoap.transport.DatagramSocketTransport;
```

#### Package Mapping Reference

| Old Package (6.x) | New Package (7.0) | Primary Contents |
|---|---|---|
| `com.mbed.coap` | `opencoap.core` | `CoapConstants` |
| `com.mbed.coap.packet` | `opencoap.core` | `CoapRequest`, `CoapResponse`, `SeparateResponse`, `Code`, `Method`, `MessageType`, `ContentFormat`, `BlockOption`, `BlockSize`, `CoapOptions`, `SignalingOptions`, `Opaque` |
| `com.mbed.coap.packet` | `opencoap.codec` | `CoapPacket`, `CoapSerializer`, `RawOption`, `DataConvertingUtility`, `CoapTcpPacketSerializer`, `CoapTcpPacketConverter` |
| `com.mbed.coap.exception` | `opencoap.core` | `CoapException`, `CoapCodeException`, `CoapTimeoutException`, `CoapBlockException` |
| `com.mbed.coap.exception` | `opencoap.codec` | `CoapMessageFormatException` |
| `com.mbed.coap.exception` | `opencoap.filter` | `TooManyRequestsForEndpointException` |
| `com.mbed.coap.client` | `opencoap.endpoint` | `CoapClient` |
| `com.mbed.coap.client` | `opencoap.linkformat` | `RegistrationManager` |
| `com.mbed.coap.server` | `opencoap.endpoint` | `CoapServer`, `CoapServerBuilder`, `CoapServerGroup`, `TcpCoapServer`, `CoapMessageKey` |
| `com.mbed.coap.server` | `opencoap.routing` | `RoutingHandler` |
| `com.mbed.coap.server` | `opencoap.observe` | `ObservationHandler`, `ObserveRequestFilter`, `NotificationValidator` |
| `com.mbed.coap.server.messaging` | `opencoap.endpoint` | `CoapDispatcher`, `CoapTcpDispatcher`, `Capabilities`, `MessageIdSupplier`, `RequestTagSupplier` |
| `com.mbed.coap.server.messaging` | `opencoap.endpoint.pipeline` | `ExchangeFilter`, `PiggybackedExchangeFilter`, `TcpExchangeFilter`, `DuplicateDetector`, `CriticalOptionVerifier`, `RescueFilter`, `RetransmissionFilter`, `MaxMessageSizeFilter` |
| `com.mbed.coap.server.block` | `opencoap.endpoint.pipeline` | `BlockWiseIncomingFilter`, `BlockWiseOutgoingFilter`, `BlockWiseNotificationFilter`, `BlockWiseTransfer` |
| `com.mbed.coap.server.filter` | `opencoap.filter` | `CongestionControlFilter`, `EchoFilter`, `EtagGeneratorFilter`, `EtagValidatorFilter`, `MaxAllowedPayloadFilter`, `RequestLoggerFilter`, `ResponseTimeoutFilter`, `TokenGeneratorFilter` |
| `com.mbed.coap.server.observe` | `opencoap.observe` | `HashMapObservationsStore`, `NotificationsReceiver`, `ObservationsStore`, `ObserversManager` |
| `com.mbed.coap.transmission` | `opencoap.endpoint` | `RetransmissionBackOff` |
| `com.mbed.coap.transport` | `opencoap.core` | `MessageAttributes`, `AttributeKey` |
| `com.mbed.coap.transport` | `opencoap.transport` | `CoapTransport`, `BlockingCoapTransport`, `CoapTcpTransport`, `CoapTcpListener`, `LoggingCoapTransport` |
| `com.mbed.coap.transport.udp` | `opencoap.transport` | `DatagramSocketTransport` |
| `com.mbed.coap.transport.javassl` | `opencoap.transport` | `SocketClientTransport`, `SSLSocketClientTransport` |
| `com.mbed.coap.transport.stdio` | `opencoap.transport` | `StreamBlockingTransport`, `OpensslProcessTransport` |
| `com.mbed.coap.linkformat` | `opencoap.linkformat` | `LinkFormat`, `LinkFormatParser` (was `LinkFormatBuilder`), `PToken` |
| `com.mbed.coap.utils` | `opencoap.core` | `Handler`, `Filter`, `MappingFilter` |
| `com.mbed.coap.utils` | `opencoap.util` | `FutureHelpers`, `ExecutorHelpers`, `Scheduler`, `Validations` |
| `org.opencoap.coap.netty` | `opencoap.transport` | `NettyCoapTransport`, `CoapCodec`, `NettyUtils` |
| `org.opencoap.transport.mbedtls` | `opencoap.transport` | `MbedtlsCoapTransport`, `DtlsSessionSuspensionHandler`, `DtlsAttributes` |
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

The 4-argument `SeparateResponse` constructor and `toSeparate(..., TransportContext)` overloads have been removed. Set attributes on the response prior to converting to separate response, or use `modify()`:

```diff
-SeparateResponse sep = response.toSeparate(token, peerAddress, transContext);
+SeparateResponse sep = response.withAttributes(attributes).toSeparate(token, peerAddress);
```

```diff
-SeparateResponse sep = new SeparateResponse(response, token, peerAddress, transContext);
+SeparateResponse sep = new SeparateResponse(response.withAttributes(attributes), token, peerAddress);
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

#### On CoapOptions

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

#### Signaling Options (RFC 8323 spelling)

`SignallingHeaderOptions` is renamed to `SignalingCoapOptions`. The spelling now matches RFC 8323 and the existing `SignalingOptions` class, and "HeaderOptions" is replaced by `CoapOptions`, which it extends.

```diff
-import com.mbed.coap.packet.SignallingHeaderOptions;
+import opencoap.core.SignalingCoapOptions;

-SignallingHeaderOptions options = new SignallingHeaderOptions(Code.C701_CSM);
-options.putSignallingOptions(signalingOptions);
-SignalingOptions sig = options.toSignallingOptions(Code.C701_CSM);
+SignalingCoapOptions options = new SignalingCoapOptions(Code.C701_CSM);
+options.putSignalingOptions(signalingOptions);
+SignalingOptions sig = options.toSignalingOptions(Code.C701_CSM);
```

#### Handler and Filter Renames

`Service` is renamed to `Handler`, and `Filter` / `SimpleFilter` are inverted into `MappingFilter` / `Filter`. See [Handler and Filter Types](#8-handler-and-filter-types).

#### Header Options Unification

`BasicHeaderOptions` and `HeaderOptions` are merged into a single `CoapOptions` class. See [Header Options Unification and Framing Extraction](#7-header-options-unification-and-framing-extraction).

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

#### Wire Code Decoding

`Method`, `MessageType` and `Code` decoded wire values with a static `valueOf(int)`, which was easy to confuse with the enum's `valueOf(String)` lookup by name. These are renamed to `fromCode`, with no change in behaviour:

| 6.x | 7.0 | Unknown value |
|---|---|---|
| `Method.valueOf(int)` | `Method.fromCode(int)` | throws `CoapException` |
| `MessageType.valueOf(int)` | `MessageType.fromCode(int)` | throws `CoapException` |
| `Code.valueOf(int)` | `Code.fromCode(int)` | returns `null` |
| `Code.valueOf(int codeClass, int codeDetail)` | `Code.fromCode(int codeClass, int codeDetail)` | returns `null` |

```diff
-Code code = Code.valueOf(2, 5);
+Code code = Code.fromCode(2, 5);
```

#### CLI Transport Codec

```diff
-opencoap.cli.transport.CoapSerializer
+opencoap.cli.transport.CoapPacketCodec
```

#### Misleading Name Renames

Classes whose names described something other than what they do:

| 6.x | 7.0 | Why |
|---|---|---|
| `CoapRequestId` | `CoapMessageKey` | It is the duplicate-detection cache key (message id + source address), not a request identifier. |
| `Timer` | `Scheduler` | Avoids confusion with `java.util.Timer` and Micrometer's `Timer`. |
| `Timer.toTimer(ScheduledExecutorService)` | `Scheduler.toScheduler(ScheduledExecutorService)` | Follows the type rename. |
| `CoapServerBuilderForTcp` | `TcpCoapServerBuilder` | Matches `TcpCoapServer`, which creates it. |
| `PayloadSizeVerifier` | `MaxMessageSizeFilter` | It is a filter that rejects packets larger than the CSM-negotiated max message size. |
| `Validations.assume(...)` | `Validations.check(...)` | `assume` suggests JUnit's skip-test semantics; the method throws `IllegalStateException`. Pairs with `require(...)`, which throws `IllegalArgumentException`. |

A custom duplicate-detection cache now uses the new key type:

```diff
-PutOnlyMap<CoapRequestId, CoapPacket> cache = ...;
+PutOnlyMap<CoapMessageKey, CoapPacket> cache = ...;
 CoapServer.builder().duplicateMessageDetectorCache(cache);
```

Filters that take a scheduler:

```diff
-Timer timer = Timer.toTimer(executor);
-new ResponseTimeoutFilter<>(timer, req -> Duration.ofSeconds(5));
+Scheduler scheduler = Scheduler.toScheduler(executor);
+new ResponseTimeoutFilter<>(scheduler, req -> Duration.ofSeconds(5));
```

Test fixtures: `MockTimer` &rarr; `MockScheduler`.

---

### 6. Content-Format and Option Types (uint16)

#### uint16 Content-Format Representation

RFC 7252 defines Content-Format identifiers as unsigned 16-bit integers (`uint16`, range `0..65535`). Previously, `java-coap` represented them as signed `short` / `Short` (`-32768..32767`). As a consequence, any content format &ge; 32768 (the upper half of the IANA registry, including the 65000+ experimental range) was truncated to a negative number during parsing and serialized incorrectly.

Content format is now uniformly represented as `int` / `Integer`:

- **Constants:** `ContentFormat` constants are `public static final int`.
- **Options & Builders:** `CoapOptions`, `CoapOptionsBuilder`, `CoapRequest.Builder`, and `CoapResponse.Builder` accept and return `int` / `Integer` for `contentFormat` and `accept`.
- **Range validation:** `CoapOptions.setContentFormat(Integer)` and `setAccept(Integer)` validate that values fall within `0..65535` (`0xFFFF`), throwing `IllegalArgumentException` otherwise.
- **Removed overload trap:** The `CoapOptions.setAccept(short)` overload has been removed to eliminate ambiguity with `setAccept(Integer)`.
- **LinkFormat:** `LinkFormat.getContentType()` and `setContentType(...)` are renamed to `getContentFormat()` and `setContentFormat(Integer)`, and use `Integer` instead of `Short`.
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
+Integer ct = linkFormat.getContentFormat();
```

#### Option Numbers and Constant Type Corrections

Other option-related constants and fields that could not represent their full domains have also been corrected:

- **Option number constants:** `CoapOptions` option constants (`IF_MATCH`, `URI_HOST`, `ETAG`, `IF_NONE_MATCH`, `URI_PORT`, `LOCATION_PATH`, `URI_PATH`, `CONTENT_FORMAT`, `MAX_AGE`, `URI_QUERY`, `ACCEPT`, `LOCATION_QUERY`, `PROXY_URI`, `PROXY_SCHEME`, `SIZE1`) changed from `byte` to `int` (CoAP option numbers are unsigned integers).
- **Default Max-Age:** `CoapOptions.DEFAULT_MAX_AGE` changed from `short` (`60`) to `long` (`60L`), matching the `Long maxAge` field.
- **Max retransmit:** `CoapConstants.MAX_RETRANSMIT` changed from `Short` to primitive `int` (`4`).

---

### 7. Header Options Unification and Framing Extraction

#### Unification of BasicHeaderOptions and HeaderOptions into CoapOptions

In 6.x, options were split across `BasicHeaderOptions` (base RFC 7252 options) and `HeaderOptions` (extended options: Observe, Block1, Block2, Size2, Echo, Request-Tag, Correlation-Tag). In practice, `BasicHeaderOptions` was only used as the superclass of `HeaderOptions` and was not instantiated anywhere in application code.

In 7.0, both classes are unified into a single `CoapOptions` class in `opencoap.core`. Packet structures and builders now consistently produce and consume `CoapOptions`:

- `CoapRequest.options()` and `CoapResponse.options()` return `CoapOptions`.
- `CoapPacket.headers()` and `setHeaderOptions(...)` are renamed to `options()` and `setOptions(CoapOptions)`.
- `CoapOptionsBuilder.build()` returns `CoapOptions`.
- `SignallingHeaderOptions` is renamed to `SignalingCoapOptions` and extends `CoapOptions` directly.

```diff
-import com.mbed.coap.packet.HeaderOptions;
-import com.mbed.coap.packet.BasicHeaderOptions;
+import opencoap.core.CoapOptions;

-HeaderOptions options = new HeaderOptions();
+CoapOptions options = new CoapOptions();
```

```diff
-HeaderOptions options = request.options();
+CoapOptions options = request.options();
```

##### SignalingCoapOptions Duplication Fix

Because `HeaderOptions` did not override `duplicate(HeaderOptions)`, `SignallingHeaderOptions.duplicate()` previously invoked `super.duplicate(BasicHeaderOptions)`, silently dropping all extended option fields (Observe, Block1, Block2, Size2, Echo, Request-Tag, and Correlation-Tag) during duplication. With the unified `CoapOptions`, `duplicate()` now correctly copies all options.

#### Option Wire Framing Extracted to CoapSerializer

Byte-level wire serialization and deserialization of options have moved from the options class to static methods on `opencoap.codec.CoapSerializer`. `CoapOptions` now solely represents the header data model, while `CoapSerializer` handles wire encoding and decoding for both UDP and TCP transports.

- `CoapOptions.serialize(OutputStream)` &rarr; `CoapSerializer.serializeOptions(CoapOptions, OutputStream)`
- `CoapOptions.deserialize(InputStream, int)` &rarr; `CoapSerializer.deserializeOptions(CoapOptions, InputStream, int)`
- Added `CoapSerializer.deserializeOptions(CoapOptions, InputStream)` which reads all available bytes from the stream and returns a `boolean` indicating whether a payload marker (`0xFF`) was present.

```diff
+import opencoap.codec.CoapSerializer;
 import opencoap.core.CoapOptions;

 CoapOptions options = new CoapOptions();
-options.serialize(outputStream);
+CoapSerializer.serializeOptions(options, outputStream);
```

```diff
+import opencoap.codec.CoapSerializer;
 import opencoap.core.CoapOptions;

 CoapOptions options = new CoapOptions();
-options.deserialize(inputStream, availableBytes);
+CoapSerializer.deserializeOptions(options, inputStream, availableBytes);
```

---

### 8. Handler and Filter Types

#### Service Renamed to Handler

`Service` is a heavily overloaded term in Java backends (Spring, microservices). The request-to-response function is now called `Handler`, in line with conventions such as http4k and Netty. The interface shape is unchanged: `(REQ) -> CompletableFuture<RES>`.

```diff
-import com.mbed.coap.utils.Service;
+import opencoap.core.Handler;

-Service<CoapRequest, CoapResponse> resource = req -> CoapResponse.ok("hello").toFuture();
+Handler<CoapRequest, CoapResponse> resource = req -> CoapResponse.ok("hello").toFuture();
```

Implementors whose names repeated the interface name were renamed as well:

| Old (6.x) | New (7.0) |
|---|---|
| `RouterService` | `RoutingHandler` |
| `RouterService.NOT_FOUND_SERVICE` | `RoutingHandler.NOT_FOUND` |
| `DtlsSessionSuspensionService` | `DtlsSessionSuspensionHandler` |

```diff
-import com.mbed.coap.server.RouterService;
+import opencoap.routing.RoutingHandler;

 CoapServer.builder()
-        .route(RouterService.builder()
+        .route(RoutingHandler.builder()
                 .get("/sensors/temperature", req -> CoapResponse.ok("21C").toFuture())
         )
```

```diff
-import org.opencoap.transport.mbedtls.DtlsSessionSuspensionService;
+import opencoap.transport.DtlsSessionSuspensionHandler;

-Service<CoapRequest, CoapResponse> suspend = new DtlsSessionSuspensionService();
+Handler<CoapRequest, CoapResponse> suspend = new DtlsSessionSuspensionHandler();
```

Methods that returned services are renamed as well: `CoapServer.clientService()` is now `outboundHandler()` and `CoapServer.outboundResponseService()` is now `notificationHandler()`. See [Renamed Methods](#11-renamed-methods).

#### Filter and MappingFilter

Most filters keep the request and response types unchanged, so the short name now belongs to that case. The general form, which can change types across a layer boundary (e.g. `CoapRequest` &rarr; `CoapPacket`), is now `MappingFilter`.

| Old (6.x) | New (7.0) |
|---|---|
| `Filter<REQ, RES, IN_REQ, IN_RES>` | `MappingFilter<REQ, RES, IN_REQ, IN_RES>` |
| `Filter.SimpleFilter<REQ, RES>` | `Filter<REQ, RES>` |
| `Filter.UnaryFilter<T>` | `Filter<T, T>` |
| `Filter.of(requestMapper, responseMapper)` | `MappingFilter.of(requestMapper, responseMapper)` |

`Filter<REQ, RES>` extends `MappingFilter<REQ, RES, REQ, RES>`, so every filter can still be composed with mapping filters. `andThenMap(...)` and `then(...)` are defined on `MappingFilter` and remain available on `Filter`. `Filter.identity()` returns a `Filter<REQ, RES>`, and `Filter.andThen(Filter)` returns a `Filter`.

Type-preserving filters:

```diff
-import com.mbed.coap.utils.Filter;
+import opencoap.core.Filter;

-public class MyFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
+public class MyFilter implements Filter<CoapRequest, CoapResponse> {

     @Override
-    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
+    public CompletableFuture<CoapResponse> apply(CoapRequest request, Handler<CoapRequest, CoapResponse> handler) {
```

Filters declared with four identical-pair type parameters collapse to two:

```diff
-Filter<CoapRequest, CoapResponse, CoapRequest, CoapResponse> filter = ...;
+Filter<CoapRequest, CoapResponse> filter = ...;
```

Type-changing filters:

```diff
-import com.mbed.coap.utils.Filter;
+import opencoap.core.MappingFilter;

-public class MyMapper implements Filter<CoapRequest, CoapResponse, CoapPacket, CoapPacket> {
+public class MyMapper implements MappingFilter<CoapRequest, CoapResponse, CoapPacket, CoapPacket> {
```

```diff
-Filter.of(CoapPacket::from, CoapPacket::toCoapResponse)
+MappingFilter.of(CoapPacket::from, CoapPacket::toCoapResponse)
```

##### Builder Filter Parameters

`CoapServerBuilder` and `TcpCoapServerBuilder` now take the same type for their filter hooks. Previously the UDP builder took `Filter<CoapRequest, CoapResponse, CoapRequest, CoapResponse>` while the TCP builder took `Filter.SimpleFilter<CoapRequest, CoapResponse>`. Both now take `Filter<CoapRequest, CoapResponse>`:

- `CoapServerBuilder.routeFilter(...)`, `inboundRequestFilter(...)`, `outboundFilter(...)`
- `TcpCoapServerBuilder.routeFilter(...)`, `outboundFilter(...)`
- `RoutingHandler.RouteBuilder.filter(...)`

Lambdas passed directly need no change. Variables or classes typed as the 4-parameter form must be changed to `Filter<CoapRequest, CoapResponse>`, because a `MappingFilter` is not accepted where a `Filter` is expected.

---

### 9. Message Attributes

#### TransportContext Renamed to MessageAttributes

`TransportContext` carried more than transport facts: besides DTLS session data it holds CoAP-layer hints (`NON_CONFIRMABLE`, `RESPONSE_TIMEOUT`) and arbitrary application data passed through the stack. It is now `MessageAttributes`, which contrasts with `CoapOptions`: options are serialized on the wire, attributes never are. The package is unchanged.

| Old (6.x) | New (7.0) |
|---|---|
| `TransportContext` | `MessageAttributes` |
| `TransportContext.Key<T>` | `AttributeKey<T>` |
| `DtlsTransportContext` | `DtlsAttributes` |
| `DtlsTransportContext.toTransportContext(...)` | `DtlsAttributes.toAttributes(...)` |

Accessors on `CoapRequest`, `CoapResponse`, `SeparateResponse` and `CoapPacket` now use the same spelling:

| Old (6.x) | New (7.0) |
|---|---|
| `getTransContext()`, `CoapPacket.getTransportContext()` | `getAttributes()` |
| `getTransContext(key)`, `getTransContext(key, default)` | `getAttribute(key)`, `getAttribute(key, default)` |
| `CoapPacket.setTransportContext(...)` | `CoapPacket.setAttributes(...)` |
| `CoapResponse.withContext(...)` | `CoapResponse.withAttributes(...)` |
| `Builder.context(...)` | `Builder.attributes(...)` |
| `Builder.addContext(key, value)` | `Builder.addAttribute(key, value)` |
| `Builder.addContext(context)` | `Builder.addAttributes(attributes)` |

`CoapResponse.getAttribute(key, default)` and `SeparateResponse.Builder.addAttributes(...)` are new, so all three message types offer the same accessors.

```diff
-import com.mbed.coap.transport.TransportContext;
+import opencoap.core.MessageAttributes;

 CoapRequest.post("/actuator/switch")
-        .addContext(TransportContext.RESPONSE_TIMEOUT, Duration.ofMinutes(3))
+        .addAttribute(MessageAttributes.RESPONSE_TIMEOUT, Duration.ofMinutes(3))
         .build();

-Duration timeout = request.getTransContext(TransportContext.RESPONSE_TIMEOUT);
+Duration timeout = request.getAttribute(MessageAttributes.RESPONSE_TIMEOUT);
```

```diff
-import org.opencoap.transport.mbedtls.DtlsTransportContext;
+import opencoap.transport.DtlsAttributes;

-String subject = request.getTransContext(DtlsTransportContext.DTLS_PEER_CERTIFICATE_SUBJECT);
+String subject = request.getAttribute(DtlsAttributes.DTLS_PEER_CERTIFICATE_SUBJECT);
```

Transport resolvers passed to `NettyCoapTransport` and `CoapCodec` are now typed `Function<DatagramPacket, MessageAttributes>`.

#### AttributeKey Factories

The public `Key(defaultValue)` constructor is replaced by named factories. The name is a debug label, shown by `toString()` on both `AttributeKey` and `MessageAttributes`. Keys are still compared by identity, so two keys with the same name are different keys.

| Factory | Value returned by `get(key)` when the key is absent |
|---|---|
| `AttributeKey.defaulted(name, defaultValue)` | `defaultValue` (must not be null) |
| `AttributeKey.optional(name)` | `null` |
| `AttributeKey.required(name)` | throws `IllegalStateException` naming the key |

`getOrDefault(key, default)` never throws, whatever factory created the key.

```diff
-static final TransportContext.Key<String> TENANT = new TransportContext.Key<>(null);
-static final TransportContext.Key<Boolean> TRACED = new TransportContext.Key<>(false);
+static final AttributeKey<String> TENANT = AttributeKey.optional("TENANT");
+static final AttributeKey<Boolean> TRACED = AttributeKey.defaulted("TRACED", false);
```

`MessageAttributes.keys()` no longer includes a `null` element for `MessageAttributes.EMPTY`.

### 10. Reduced Visibility

These were public in 6.x but are internal implementation details, not meant to be called by applications. They are now package-private:

| 6.x | 6.x visibility | 7.0 |
|---|---|---|
| `HeaderOptions.parseOption(int, Opaque)` | public (`protected` in `BasicHeaderOptions`) | `CoapOptions.parseOption(int, Opaque)`, package-private. Add custom options with `put(int, Opaque)` and read them with `getCustomOption(Integer)` instead of overriding it |
| `BlockOption.toBytes()` | public | package-private |
| `CoapSerializer.writeCode(OutputStream, CoapPacket)` | public | package-private |
| `org.opencoap.coap.netty.NettyUtils` | public class | `opencoap.transport.NettyUtils`, package-private class |

`BlockingCoapTransport.sendPacket0(CoapPacket)` is now `protected`. It is the template method that subclasses implement, and callers should use `sendPacket(CoapPacket)`. Subclasses can keep declaring their override `public`, but `protected` is recommended:

```diff
 class MyTransport extends BlockingCoapTransport {
     @Override
-    public void sendPacket0(CoapPacket coapPacket) throws CoapException, IOException {
+    protected void sendPacket0(CoapPacket coapPacket) throws CoapException, IOException {
         // ...
     }
 }
```

### 11. Renamed Methods

Methods whose names were left over from earlier renames, misspelled, or inconsistent with the rest of the API are renamed. The old names are removed.

| 6.x | 7.0 | Why |
|---|---|---|
| `CoapServer.clientService()` | `CoapServer.outboundHandler()` | Left over from the `Service` &rarr; `Handler` rename |
| `CoapServer.outboundResponseService()` | `CoapServer.notificationHandler()` | Left over from the `Service` &rarr; `Handler` rename. It is the pipeline that sends observation notifications |
| `CoapClient.clientService` (protected field) | `CoapClient.outboundHandler` | Follows `CoapServer.outboundHandler()` |
| `CoapPacket.headers()` | `CoapPacket.options()` | Matches `CoapRequest`, `CoapResponse` and `SeparateResponse` |
| `CoapPacket.setHeaderOptions(CoapOptions)` | `CoapPacket.setOptions(CoapOptions)` | Same |
| `CoapOptions.isUnsave(int)` | `CoapOptions.isUnsafe(int)` | Typo. RFC 7252 §5.4.2 calls it "Unsafe" |
| `CoapOptions.IF_NON_MATCH` | `CoapOptions.IF_NONE_MATCH` | The option is called If-None-Match (RFC 7252 §5.10.8.2) |
| `CoapOptions.getIfNonMatch()` / `setIfNonMatch(Boolean)` | `CoapOptions.getIfNoneMatch()` / `setIfNoneMatch(Boolean)` | Same |
| `CoapOptionsBuilder.ifNonMatch()` | `CoapOptionsBuilder.ifNoneMatch()` | Same |
| `CoapOptions.containsUnrecognisedCriticalOption(...)` | `CoapOptions.containsUnrecognizedCriticalOption(...)` | American spelling, as in `CoapServerBuilder.recognizedCustomOptions` |
| `Capabilities.isBERTEnabled()` | `Capabilities.isBertEnabled()` | Matches `BlockOption.isBert()` |
| `LinkFormat.setOAutobservable(Boolean)` | `LinkFormat.setAutoObservable(Boolean)` | Typo |
| `LinkFormat.getMaxSize()` | `LinkFormat.getMaximumSize()` | Both read the `sz` attribute. Only the one matching `setMaximumSize` is kept |
| `LinkFormat.getContentType()` / `setContentType(Integer)` | `LinkFormat.getContentFormat()` / `setContentFormat(Integer)` | The `ct` attribute is a CoAP Content-Format |
| `MessageIdSupplier.getNextMID()` | `MessageIdSupplier.next()` | Matches `RequestTagSupplier.next()` |
| `RequestTagSupplier.createSequential(...)` | `RequestTagSupplier.sequential(...)` | Matches `MessageIdSupplier.sequential(...)` |
| `CoapServerBuilder.midSupplier(...)` | `CoapServerBuilder.messageIdSupplier(...)` | Named after the `MessageIdSupplier` type, like `requestTagSupplier(...)` |
| `SignallingHeaderOptions` | `SignalingCoapOptions` | See [Signaling Options](#signaling-options-rfc-8323-spelling) |

```diff
-CoapOptions options = packet.headers();
-packet.setHeaderOptions(options);
+CoapOptions options = packet.options();
+packet.setOptions(options);
```

```diff
 CoapServer.builder()
-        .midSupplier(MessageIdSupplier.sequential(0))
-        .requestTagSupplier(RequestTagSupplier.createSequential(100))
+        .messageIdSupplier(MessageIdSupplier.sequential(0))
+        .requestTagSupplier(RequestTagSupplier.sequential(100))
```

A custom `MessageIdSupplier` implements `next()`:

```diff
 class MyMessageIdSupplier implements MessageIdSupplier {
     @Override
-    public int getNextMID() {
+    public int next() {
         // ...
     }
 }
```

#### LinkFormatBuilder Renamed to LinkFormatParser

`LinkFormatBuilder` was not a builder but a set of static parse and format helpers, so it is renamed to `LinkFormatParser`. The two list parsers are merged into one `parse` that returns a `List`, and `toString(Collection)`, which reused `Object.toString`'s name, is now `format`:

| 6.x | 7.0 |
|---|---|
| `LinkFormatBuilder.parseList(String)` (returns `LinkFormat[]`) | `LinkFormatParser.parse(String)` (returns `List<LinkFormat>`) |
| `LinkFormatBuilder.parseLinkAsList(String)` | `LinkFormatParser.parse(String)` |
| `LinkFormatBuilder.parse(String)` (single link) | `LinkFormatParser.parse(String).get(0)` |
| `LinkFormatBuilder.toString(Collection<LinkFormat>)` | `LinkFormatParser.format(Collection<LinkFormat>)` |
| `LinkFormatBuilder.filter(List<LinkFormat>, Map<String, String>)` | `LinkFormatParser.filter(List<LinkFormat>, Map<String, String>)` |

```diff
-import opencoap.linkformat.LinkFormatBuilder;
+import opencoap.linkformat.LinkFormatParser;

-LinkFormat[] links = LinkFormatBuilder.parseList(payload);
-String text = LinkFormatBuilder.toString(Arrays.asList(links));
+List<LinkFormat> links = LinkFormatParser.parse(payload);
+String text = LinkFormatParser.format(links);
```
