/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
 * Copyright (C) 2011-2021 ARM Limited. All rights reserved.
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

import static com.mbed.coap.utils.FutureHelpers.wrapExceptions;
import com.mbed.coap.transport.TransportContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class CoapRequest {
    private final Method method;
    private final Opaque token;
    private final HeaderOptions options;
    private final Opaque payload;
    private final InetSocketAddress peerAddress;
    private final TransportContext transContext;

    public CoapRequest(Method method, Opaque token, HeaderOptions options, Opaque payload, InetSocketAddress peerAddress, TransportContext transContext) {
        this.method = Objects.requireNonNull(method);
        this.token = Objects.requireNonNull(token);
        this.options = Objects.requireNonNull(options);
        this.payload = Objects.requireNonNull(payload);
        this.peerAddress = peerAddress;
        this.transContext = Objects.requireNonNull(transContext);
    }

    private CoapRequest(InetSocketAddress peerAddress, TransportContext transContext) {
        // ping
        this.method = null;
        this.token = Opaque.EMPTY;
        this.options = new HeaderOptions();
        this.payload = Opaque.EMPTY;
        this.peerAddress = Objects.requireNonNull(peerAddress);
        this.transContext = Objects.requireNonNull(transContext);
    }


    // --- STATIC BUILDERS ---
    public static Builder request(Method method, String uriPath) {
        return new Builder(method, uriPath);
    }

    public static Builder get(String uriPath) {
        return request(Method.GET, uriPath);
    }

    public static Builder put(String uriPath) {
        return request(Method.PUT, uriPath);
    }

    public static Builder post(String uriPath) {
        return request(Method.POST, uriPath);
    }

    public static Builder delete(String uriPath) {
        return request(Method.DELETE, uriPath);
    }

    public static Builder fetch(String uriPath) {
        return request(Method.FETCH, uriPath);
    }

    public static Builder patch(String uriPath) {
        return request(Method.PATCH, uriPath);
    }

    public static Builder iPatch(String uriPath) {
        return request(Method.iPATCH, uriPath);
    }

    public static Builder observe(String uriPath) {
        return get(uriPath).observe();
    }

    public static CoapRequest ping(InetSocketAddress peerAddress, TransportContext transContext) {
        return new CoapRequest(peerAddress, transContext);
    }

    // --------------------


    public Method getMethod() {
        return method;
    }

    public Opaque getToken() {
        return token;
    }

    public HeaderOptions options() {
        return options;
    }

    public Opaque getPayload() {
        return payload;
    }

    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public TransportContext getTransContext() {
        return transContext;
    }

    public <T> T getTransContext(TransportContext.Key<T> key) {
        return transContext.get(key);
    }

    public <T> T getTransContext(TransportContext.Key<T> key, T defaultValue) {
        return transContext.getOrDefault(key, defaultValue);
    }

    public boolean isPing() {
        return method == null && token.isEmpty() && payload.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoapRequest that = (CoapRequest) o;
        return method == that.method && Objects.equals(token, that.token) && Objects.equals(options, that.options) && Objects.equals(payload, that.payload) && Objects.equals(peerAddress, that.peerAddress) && Objects.equals(transContext, that.transContext);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, options, peerAddress, transContext);
        result = 31 * result + Objects.hashCode(token);
        result = 31 * result + Objects.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        if (method == null) {
            return "CoapRequest[PING]";
        }
        if (payload.isEmpty() && token.isEmpty()) {
            return String.format("CoapRequest[%s%s]", method, options);
        } else if (payload.isEmpty()) {
            return String.format("CoapRequest[%s%s,Token:%s]", method, options, token.toHex());
        } else if (token.isEmpty()) {
            return String.format("CoapRequest[%s%s, pl(%d):%s]", method, options, payload.size(), payload.toHexShort(20));
        }
        return String.format("CoapRequest[%s%s,Token:%s, pl(%d):%s]", method, options, token.toHex(), payload.size(), payload.toHexShort(20));
    }


    // ---  MODIFIERS ---
    public CoapRequest withToken(Opaque newToken) {
        return new CoapRequest(method, newToken, options, payload, peerAddress, transContext);
    }

    public CoapRequest withOptions(Consumer<CoapOptionsBuilder> optionsFunc) {
        CoapOptionsBuilder optionsBuilder = CoapOptionsBuilder.from(options);
        optionsFunc.accept(optionsBuilder);
        return new CoapRequest(method, token, optionsBuilder.build(), payload, peerAddress, transContext);
    }

    public CoapRequest withPayload(Opaque newPayload) {
        return new CoapRequest(method, token, options, newPayload, peerAddress, transContext);
    }

    public CoapRequest withAddress(InetSocketAddress newPeerAddress) {
        return new CoapRequest(method, token, options, payload, newPeerAddress, transContext);
    }

    public static class Builder {
        private final Method method;
        private Opaque token = Opaque.EMPTY;
        private final CoapOptionsBuilder options;
        private Opaque payload = Opaque.EMPTY;
        private InetSocketAddress peerAddress;
        private TransportContext transContext = TransportContext.EMPTY;

        private Builder(Method method, String uriPath) {
            this.method = method;
            this.options = CoapOptionsBuilder.options();
            this.options.uriPath(uriPath);
        }

        public CoapRequest build() {
            return new CoapRequest(method, token, options.build(), payload, peerAddress, transContext);
        }

        public CoapRequest to(InetSocketAddress address) {
            return this.address(address).build();
        }

        public CoapRequest toLocal(int localPort) {
            return wrapExceptions(() ->
                    this.address(new InetSocketAddress(InetAddress.getLocalHost(), localPort)).build()
            );
        }

        public CoapRequest from(InetSocketAddress address) {
            return this.address(address).build();
        }

        public CoapRequest fromLocal(int localPort) {
            return wrapExceptions(() ->
                    this.address(new InetSocketAddress(InetAddress.getLocalHost(), localPort)).build()
            );
        }

        public Builder payload(Opaque payload, short contentFormat) {
            this.payload = payload;
            return contentFormat(contentFormat);
        }

        public Builder payload(String payload, short contentFormat) {
            return payload(Opaque.of(payload), contentFormat);
        }

        public Builder payload(String newPayload) {
            return payload(Opaque.of(newPayload));
        }

        public Builder payload(Opaque payload) {
            this.payload = payload;
            return this;
        }

        public Builder token(Opaque token) {
            this.token = token;
            return this;
        }

        public Builder token(long token) {
            return token(Opaque.variableUInt(token));
        }

        public Builder context(TransportContext newTransportContext) {
            this.transContext = newTransportContext;
            return this;
        }

        public <T> Builder context(TransportContext.Key<T> key, T value) {
            transContext = transContext.with(key, value);
            return this;
        }

        public Builder address(InetSocketAddress address) {
            this.peerAddress = address;
            return this;
        }

        public Builder options(Consumer<CoapOptionsBuilder> optionsFunc) {
            optionsFunc.accept(options);
            return this;
        }

        public Builder observe() {
            options.observe(0);
            return this;
        }

        public Builder deregisterObserve() {
            options.observe(1);
            return this;
        }


        public Builder accept(Short contentFormat) {
            options.accept(contentFormat);
            return this;
        }

        public Builder blockSize(BlockSize size) {
            if (size == null) {
                return this;
            }
            if (method == Method.GET) {
                options.block2Res(0, size, false);
            }
            if (method == Method.PUT || method == Method.POST || method == Method.FETCH || method == Method.PATCH || method == Method.iPATCH) {
                options.block1Req(0, size, true);
            }
            return this;
        }

        public Builder block1Req(int blockNr, BlockSize size, boolean more) {
            options.block1Req(blockNr, size, more);
            return this;
        }

        public Builder block2Res(int blockNr, BlockSize blockSize, boolean more) {
            options.block2Res(blockNr, blockSize, more);
            return this;
        }

        public Builder size1(int size) {
            options.size1(size);
            return this;
        }

        public Builder etag(Opaque... etag) {
            options.etag(etag);
            return this;
        }

        public Builder query(String query) {
            options.query(query);
            return this;
        }

        public Builder maxAge(Duration maxAge) {
            options.maxAge(maxAge);
            return this;
        }

        public Builder host(String host) {
            options.host(host);
            return this;
        }

        public Builder query(String name, String value) {
            options.query(name, value);
            return this;
        }

        public Builder contentFormat(Short contentFormat) {
            options.contentFormat(contentFormat);
            return this;
        }
    }
}
