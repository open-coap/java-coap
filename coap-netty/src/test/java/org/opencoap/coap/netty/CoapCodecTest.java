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

import static com.mbed.coap.utils.Bytes.opaqueOfRandom;
import static com.mbed.coap.utils.Networks.localhost;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.TransportContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoapCodecTest {
    private EmbeddedChannel channel = new EmbeddedChannel();

    public static ByteBuf encodeToBuf(CoapPacket coap) {
        return wrappedBuffer(CoapSerializer.serialize(coap));
    }

    private final static TransportContext.Key<String> DUMMY_KEY = new TransportContext.Key<>(null);

    @BeforeEach
    void setUp() {
        channel.pipeline().addLast("coap-codec", new CoapCodec(dgram -> TransportContext.of(DUMMY_KEY, "recipient.port:" + dgram.recipient().getPort())));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.close().await();
    }

    @Test
    void shouldDecodeCoap() {
        CoapPacket coap = newCoapPacket(123).get().uriPath("/test").context(TransportContext.of(DUMMY_KEY, "recipient.port:5684")).build();
        ByteBuf buf = encodeToBuf(coap);
        channel.writeInbound(new DatagramPacket(buf, localhost(5684)));

        // then
        assertEquals(coap, channel.readInbound());
        assertEquals(0, buf.refCnt());
    }

    @Test
    void shouldEncode() throws Exception {
        CoapPacket coap = newCoapPacket(localhost(5684)).mid(123).get().uriPath("/test").build();
        channel.writeAndFlush(coap).get();

        DatagramPacket datagramPacket = channel.readOutbound();
        ByteBuf content = datagramPacket.content();

        assertEquals(coap, CoapSerializer.deserialize(localhost(5684), new ByteBufInputStream(content)));
    }

    @Test
    void shouldEncode_large_message() throws Exception {
        CoapPacket coap = newCoapPacket(localhost(5684)).mid(123).post().uriPath("/test").payload(opaqueOfRandom(16000)).build();
        channel.writeAndFlush(coap).get();

        DatagramPacket datagramPacket = channel.readOutbound();
        ByteBuf content = datagramPacket.content();

        assertEquals(coap, CoapSerializer.deserialize(localhost(5684), new ByteBufInputStream(content)));
    }

    @Test
    void shouldHandleMalformedMessage() {
        ByteBuf buf = wrappedBuffer("dupa".getBytes());

        assertThrows(DecoderException.class, () ->
                channel.writeInbound(new DatagramPacket(buf, localhost(5684)))
        );

        assertEquals(0, buf.refCnt());
    }

    @Test
    void shouldForwardNonDatagramPacketMessage() {
        ByteBuf buf = wrappedBuffer("dupa".getBytes());
        channel.writeInbound(buf);

        assertEquals(1, buf.refCnt());
    }
}
