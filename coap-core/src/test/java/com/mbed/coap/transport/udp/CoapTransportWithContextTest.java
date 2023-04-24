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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CoapTransportWithContextTest {

    @Test
    @Disabled
    void test() throws IOException, ExecutionException, InterruptedException {

        DatagramSocketTransport udp = DatagramSocketTransport.udp();
        CoapTransport transport = CoapTransportWithContext.wrap(udp, new InetSocketAddress("localhost", 1_5683));

        transport.start();

        CompletableFuture<CoapPacket> receivedCoap = transport.receive();
        receivedCoap.get()
                .getTransportContext().get(CoapTransportWithContext.SERVER_SOCKET); // this should return server's socket that was defined earlier


        transport.stop();
    }
}
