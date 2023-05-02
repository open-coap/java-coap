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
package org.opencoap.coap.netty;

import static com.mbed.coap.utils.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import static org.opencoap.coap.netty.CoapCodecTest.encodeToBuf;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import com.mbed.coap.packet.CoapPacket;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

public class NettyCoapTransportTest {

    private EmbeddedChannel channel = new EmbeddedChannel();

    @Test
    void should_receive() throws ExecutionException, InterruptedException {
        NettyCoapTransport transport = new NettyCoapTransport(null, EMPTY_RESOLVER);
        transport.init(channel);

        // when
        CompletableFuture<CoapPacket> receive = transport.receive();
        CoapPacket coap = newCoapPacket(123).get().uriPath("/test").build();
        channel.writeInbound(new DatagramPacket(encodeToBuf(coap), localhost(5684)));

        // then
        assertEquals(coap, receive.get());
        // and
        assertFalse(transport.receive().isDone());
    }

}
