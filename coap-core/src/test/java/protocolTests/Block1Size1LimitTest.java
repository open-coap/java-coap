/*
 * Copyright (C) 2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package protocolTests;

import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.transport.udp.DatagramSocketTransport.udp;
import static com.mbed.coap.utils.Bytes.opaqueOfSize;
import static com.mbed.coap.utils.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.packet.BlockOption;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MessageType;
import com.mbed.coap.packet.Method;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A peer declared Size1 must never drive an allocation. Both cases below are sent over a raw DatagramSocket, so that
 * nothing on the client side normalises the option before it reaches the server.
 */
public class Block1Size1LimitTest {

    private static final int DEFAULT_MAX_TRANSFER_SIZE = 10_000_000;

    private CoapServer server;
    private InetSocketAddress serverAddress;
    private DatagramSocket socket;

    @BeforeEach
    public void setUp() throws IOException {
        server = CoapServer.builder()
                .transport(udp())
                .route(RouterService.builder()
                        .get("/test/1", __ -> CoapResponse.ok("alive").toFuture())
                        .put("/test/1", __ -> coapResponse(Code.C204_CHANGED).toFuture())
                )
                .build();
        server.start();
        serverAddress = localhost(server.getLocalSocketAddress().getPort());

        socket = new DatagramSocket();
        socket.setSoTimeout(3000);
    }

    @AfterEach
    public void tearDown() {
        socket.close();
        server.stop();
    }

    @Test
    public void shouldRejectTooLargeSize1WithoutAllocatingForIt() throws Exception {
        assertEquals(Code.C205_CONTENT, exchange(probe(1)).getCode());

        // a single datagram declaring a 2 GB upload
        CoapPacket resp = exchange(firstBlock(2, Integer.MAX_VALUE));

        assertEquals(Code.C413_REQUEST_ENTITY_TOO_LARGE, resp.getCode());
        assertEquals(DEFAULT_MAX_TRANSFER_SIZE, resp.headers().getSize1().intValue());

        // the packet must not have taken the receive loop down with it
        assertEquals(Code.C205_CONTENT, exchange(probe(3)).getCode());
    }

    @Test
    public void shouldRejectUnrepresentableSize1AsClientError() throws Exception {
        CoapPacket resp = exchange(withRawNegativeSize1(firstBlock(1, Integer.MAX_VALUE)));

        assertEquals(Code.C402_BAD_OPTION, resp.getCode());
        assertEquals(Code.C205_CONTENT, exchange(probe(2)).getCode());
    }

    private CoapPacket probe(int messageId) {
        CoapPacket packet = new CoapPacket(Method.GET, MessageType.Confirmable, "/test/1", serverAddress);
        packet.setMessageId(messageId);
        packet.setToken(Opaque.decodeHex("0102"));
        return packet;
    }

    private CoapPacket firstBlock(int messageId, int size1) {
        CoapPacket packet = new CoapPacket(Method.PUT, MessageType.Confirmable, "/test/1", serverAddress);
        packet.setMessageId(messageId);
        packet.setToken(Opaque.decodeHex("aabb"));
        packet.headers().setSize1(size1);
        packet.headers().setBlock1Req(new BlockOption(0, BlockSize.S_16, true));
        packet.setPayload(opaqueOfSize(16));
        return packet;
    }

    /**
     * Size1 is read with Opaque.toInt(), which takes at most four bytes, while the typed setter would encode -1 as
     * eight bytes and be rejected already while parsing. So serialize 0x7FFFFFFF and patch its leading byte, to put
     * the raw four bytes FF FF FF FF on the wire.
     */
    private static byte[] withRawNegativeSize1(CoapPacket packet) {
        byte[] raw = CoapSerializer.serialize(packet);
        for (int i = 0; i < raw.length - 3; i++) {
            if (raw[i] == 0x7F && raw[i + 1] == -1 && raw[i + 2] == -1 && raw[i + 3] == -1) {
                raw[i] = -1;
                return raw;
            }
        }
        throw new IllegalStateException("Size1 = 0x7FFFFFFF not found in serialized packet");
    }

    private CoapPacket exchange(CoapPacket request) throws Exception {
        return exchange(CoapSerializer.serialize(request));
    }

    private CoapPacket exchange(byte[] request) throws Exception {
        socket.send(new DatagramPacket(request, request.length, serverAddress));

        DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
        socket.receive(reply);
        return CoapSerializer.deserialize(serverAddress, reply.getData(), reply.getLength());
    }
}
