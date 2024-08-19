/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
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
import java.util.Objects;

public class SeparateResponse {
    private final CoapResponse response;
    private final Opaque token;
    private final InetSocketAddress peerAddress;

    public SeparateResponse(CoapResponse response, Opaque token, InetSocketAddress peerAddress) {
        this.response = Objects.requireNonNull(response);
        this.token = Objects.requireNonNull(token);
        this.peerAddress = peerAddress;
    }

    @Deprecated
    public SeparateResponse(CoapResponse response, Opaque token, InetSocketAddress peerAddress, TransportContext transContext) {
        this(response.withContext(transContext), token, peerAddress);
    }

    @Override
    public String toString() {
        return String.format("SeparateResponse[token:%s, %s]", token, response);
    }

    public Opaque getToken() {
        return token;
    }

    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    public TransportContext getTransContext() {
        return response.getTransContext();
    }

    public <T> T getTransContext(TransportContext.Key<T> key) {
        return response.getTransContext().get(key);
    }

    public <T> T getTransContext(TransportContext.Key<T> key, T defaultValue) {
        return response.getTransContext().getOrDefault(key, defaultValue);
    }

    public Code getCode() {
        return response.getCode();
    }

    public HeaderOptions options() {
        return response.options();
    }

    public Opaque getPayload() {
        return response.getPayload();
    }

    public CoapResponse asResponse() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SeparateResponse that = (SeparateResponse) o;
        return Objects.equals(response, that.response) && Objects.equals(token, that.token) && Objects.equals(peerAddress, that.peerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response, token, peerAddress);
    }

    public SeparateResponse withPayload(Opaque newPayload) {
        return new SeparateResponse(response.withPayload(newPayload), token, peerAddress);
    }

    public SeparateResponse duplicate() {
        return new SeparateResponse(CoapResponse.of(response.getCode(), response.getPayload(), response.options().duplicate()).withContext(response.getTransContext()), token, peerAddress);
    }
}
