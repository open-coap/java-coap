/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package opencoap.cli.providers;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.SocketFactory;
import opencoap.cli.TransportProvider;
import opencoap.packet.Opaque;
import opencoap.transport.CoapTcpTransport;
import opencoap.transport.CoapTransport;
import opencoap.transport.javassl.SocketClientTransport;
import opencoap.transport.udp.DatagramSocketTransport;

public class PlainTextProvider implements TransportProvider {
    private final int bindPort;

    public PlainTextProvider(int bindPort) {
        this.bindPort = bindPort;
    }

    @Override
    public CoapTransport createUDP(InetSocketAddress destAdr, KeyStore ks, Pair<String, Opaque> psk) {
        return new DatagramSocketTransport(bindPort);
    }

    @Override
    public CoapTcpTransport createTCP(InetSocketAddress destAdr, KeyStore ks) {
        return new SocketClientTransport(destAdr, SocketFactory.getDefault(), true);
    }
}
