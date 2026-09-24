# Migration Guide

This document outlines breaking changes and migration steps between versions of `java-coap`.

- [Upgrading from 6.x to 7.0](#upgrading-from-6x-to-70)

---

## Upgrading from 6.x to 7.0

### TL;DR

- **Module removed:** The deprecated `lwm2m` module is no longer published.

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
