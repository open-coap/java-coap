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
package com.mbed.coap.transport.udp;

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.TransportContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class CoapTransportWithContext implements CoapTransport {

    private final CoapTransport underlying;
    private final InetSocketAddress serverSocket;

    public static TransportContext.Key<InetSocketAddress> SERVER_SOCKET = new TransportContext.Key<>(null);

    public CoapTransportWithContext(CoapTransport underlying, InetSocketAddress serverSocket) {
        this.underlying = underlying;
        this.serverSocket = serverSocket;
    }

    public static CoapTransport wrap(CoapTransport underlying, InetSocketAddress serverSocket) {
        return new CoapTransportWithContext(underlying, serverSocket);
    }

    @Override
    public void start() throws IOException {

    }

    @Override
    public void stop() {

    }

    @Override
    public CompletableFuture<Boolean> sendPacket(CoapPacket coapPacket) {
        return null;
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        return underlying.receive()
                .thenApply(packet -> {
                    TransportContext newTransportContext = packet.getTransportContext().with(SERVER_SOCKET, serverSocket);
                    packet.setTransportContext(newTransportContext);
                    return packet;
                });
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return null;
    }
}
