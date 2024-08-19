/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
 * SPDX-License-Identifier: Apache-2.0
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mbed.coap.packet;

import com.mbed.coap.transport.TransportContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CoapResponse {
    private final Code code;
    private final HeaderOptions options;
    private final Opaque payload;
    private final TransportContext transContext;

    private CoapResponse(Code code, Opaque payload, HeaderOptions options, TransportContext transContext) {
        this.code = code;
        this.payload = Objects.requireNonNull(payload);
        this.options = Objects.requireNonNull(options);
        this.transContext = Objects.requireNonNull(transContext);
    }

    // --- STATIC CONSTRUCTORS ---

    public static CoapResponse of(Code code) {
        return new CoapResponse(code, Opaque.EMPTY, new HeaderOptions(), TransportContext.EMPTY);
    }

    public static CoapResponse of(Code code, Opaque payload) {
        return new CoapResponse(code, payload, new HeaderOptions(), TransportContext.EMPTY);
    }

    public static CoapResponse of(Code code, Opaque payload, HeaderOptions options) {
        return new CoapResponse(code, payload, options, TransportContext.EMPTY);
    }

    public static CoapResponse of(Code code, String description) {
        return of(code, Opaque.of(description));
    }

    // --- BUILDER CONSTRUCTORS ---

    public static CoapResponse.Builder coapResponse(Code code) {
        return new CoapResponse.Builder(code);
    }

    public static CoapResponse.Builder ok() {
        return new CoapResponse.Builder(Code.C205_CONTENT);
    }

    public static CoapResponse.Builder ok(Opaque payload) {
        return new CoapResponse.Builder(Code.C205_CONTENT).payload(payload);
    }

    public static CoapResponse.Builder ok(String payload) {
        return new CoapResponse.Builder(Code.C205_CONTENT).payload(payload);
    }

    public static CoapResponse.Builder ok(String payload, short contentFormat) {
        return new CoapResponse.Builder(Code.C205_CONTENT).payload(payload).contentFormat(contentFormat);
    }

    public static CoapResponse.Builder notFound() {
        return new CoapResponse.Builder(Code.C404_NOT_FOUND);
    }

    public static CoapResponse.Builder badRequest() {
        return new CoapResponse.Builder(Code.C400_BAD_REQUEST);
    }

    // ---------------------

    public Code getCode() {
        return code;
    }

    public HeaderOptions options() {
        return options;
    }

    public Opaque getPayload() {
        return payload;
    }

    public String getPayloadString() {
        return payload.toUtf8String();
    }

    @Deprecated
    public SeparateResponse toSeparate(Opaque token, InetSocketAddress peerAddress, TransportContext transContext) {
        return new SeparateResponse(this.withContext(transContext), token, peerAddress);
    }

    public SeparateResponse toSeparate(Opaque token, InetSocketAddress peerAddress) {
        return toSeparate(token, peerAddress, TransportContext.EMPTY);
    }

    public TransportContext getTransContext() {
        return transContext;
    }

    public <T> T getTransContext(TransportContext.Key<T> key) {
        return transContext.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoapResponse that = (CoapResponse) o;
        return Objects.equals(code, that.code) && Objects.equals(options, that.options) && Objects.equals(payload, that.payload) && Objects.equals(transContext, that.transContext);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(code, options, transContext);
        result = 31 * result + Objects.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        String codeString = code != null ? code.codeToString() : "na";
        String optionsString = options.toString();
        String optionsComma = optionsString.isEmpty() ? "" : ",";

        if (payload.isEmpty()) {
            return String.format("CoapResponse[%s%s%s]", codeString, optionsComma, optionsString);
        } else {
            return String.format("CoapResponse[%s%s%s, pl(%d):%s]", codeString, optionsComma, optionsString, payload.size(), payload.toHexShort(20));
        }
    }

    // ---  IMMUTABLE MODIFIERS ---

    public CoapResponse withPayload(Opaque newPayload) {
        return new CoapResponse(code, newPayload, options, transContext);
    }

    public CoapResponse withOptions(Consumer<CoapOptionsBuilder> optionsFunc) {
        CoapOptionsBuilder optionsBuilder = CoapOptionsBuilder.from(options);
        optionsFunc.accept(optionsBuilder);
        return new CoapResponse(code, payload, optionsBuilder.build(), transContext);
    }

    public CoapResponse withContext(TransportContext otherTransContext) {
        return new CoapResponse(code, payload, options, transContext.with(otherTransContext));
    }

    public static class Builder {
        private final Code code;
        private final CoapOptionsBuilder options = CoapOptionsBuilder.options();
        private Opaque payload = Opaque.EMPTY;
        private TransportContext transContext = TransportContext.EMPTY;

        private Builder(Code code) {
            this.code = code;
        }

        public CoapResponse build() {
            return new CoapResponse(code, payload, options.build(), transContext);
        }

        public SeparateResponse toSeparate(Opaque token, InetSocketAddress peerAddress, TransportContext transContext) {
            return build().toSeparate(token, peerAddress, transContext);
        }

        public SeparateResponse toSeparate(Opaque token, InetSocketAddress peerAddress) {
            return build().toSeparate(token, peerAddress);
        }

        public CompletableFuture<CoapResponse> toFuture() {
            return CompletableFuture.completedFuture(build());
        }

        public Builder payload(Opaque payload) {
            this.payload = payload;
            return this;
        }

        public Builder payload(String payload) {
            return payload(Opaque.of(payload));
        }

        public Builder payload(String payload, short contentFormat) {
            options.contentFormat(contentFormat);
            return payload(Opaque.of(payload));
        }

        public Builder context(TransportContext newTransportContext) {
            this.transContext = newTransportContext;
            return this;
        }

        public <T> Builder addContext(TransportContext.Key<T> key, T value) {
            transContext = transContext.with(key, value);
            return this;
        }

        public <T> Builder addContext(TransportContext context) {
            transContext = transContext.with(context);
            return this;
        }

        public Builder options(Consumer<CoapOptionsBuilder> builder) {
            builder.accept(options);
            return this;
        }

        public Builder locationPath(String locationPath) {
            options.locationPath(locationPath);
            return this;
        }

        public Builder etag(Opaque etag) {
            options.etag(etag);
            return this;
        }

        public Builder block1Req(int blockNr, BlockSize blockSize, boolean more) {
            options.block1Req(blockNr, blockSize, more);
            return this;
        }

        public Builder block2Res(int blockNr, BlockSize blockSize, boolean more) {
            options.block2Res(blockNr, blockSize, more);
            return this;
        }

        public Builder maxAge(int maxAge) {
            options.maxAge(maxAge);
            return this;
        }

        public Builder maxAge(Duration maxAge) {
            options.maxAge(maxAge);
            return this;
        }

        public Builder observe(int observe) {
            options.observe(observe);
            return this;
        }

        public Builder contentFormat(short contentFormat) {
            options.contentFormat(contentFormat);
            return this;
        }

        public Builder size2Res(int size) {
            options.size2Res(size);
            return this;
        }
    }
}
