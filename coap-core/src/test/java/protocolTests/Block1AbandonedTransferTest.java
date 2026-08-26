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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A peer that opens a block-wise transfer and stops must not hold its buffer forever. Blocks are sent over raw
 * DatagramSockets, so that a transfer can be abandoned mid-way, which a client driven transfer would never do.
 */
public class Block1AbandonedTransferTest {

    private static final int BLOCK_SIZE = 16;

    private CoapServer server;
    private InetSocketAddress serverAddress;
    private final AtomicReference<Opaque> lastReceivedPayload = new AtomicReference<>();
    private final List<DatagramSocket> peers = new ArrayList<>();

    private void startServer(int maxIncomingBlockTransfers, Duration idleTimeout) throws IOException {
        server = CoapServer.builder()
                .transport(udp())
                .route(RouterService.builder()
                        .get("/test/1", __ -> CoapResponse.ok("alive").toFuture())
                        .put("/test/1", req -> {
                            lastReceivedPayload.set(req.getPayload());
                            return coapResponse(Code.C204_CHANGED).toFuture();
                        })
                )
                .maxIncomingBlockTransfers(maxIncomingBlockTransfers)
                .incomingBlockTransferIdleTimeout(idleTimeout)
                .build();
        server.start();
        serverAddress = localhost(server.getLocalSocketAddress().getPort());
    }

    @AfterEach
    public void tearDown() {
        peers.forEach(DatagramSocket::close);
        server.stop();
    }

    @Test
    public void shouldKeepOnlyBoundedNumberOfAbandonedTransfers() throws Exception {
        startServer(3, Duration.ofMinutes(5));

        // every peer sends a single first block and then goes silent
        List<DatagramSocket> abandoning = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            DatagramSocket peer = newPeer();
            abandoning.add(peer);
            assertEquals(Code.C231_CONTINUE, exchange(peer, firstBlock(1)).getCode());
        }

        // the oldest transfers are gone, only the most recent ones are still held
        assertEquals(Code.C408_REQUEST_ENTITY_INCOMPLETE, exchange(abandoning.get(0), nextBlock(2)).getCode());
        assertEquals(Code.C231_CONTINUE, exchange(abandoning.get(49), nextBlock(2)).getCode());

        // and the server keeps serving other requests
        assertEquals(Code.C205_CONTENT, exchange(newPeer(), probe(3)).getCode());
    }

    @Test
    public void shouldDropTransferThatReceivesNoBlockForLongerThanIdleTimeout() throws Exception {
        startServer(100, Duration.ofMillis(600));
        DatagramSocket peer = newPeer();

        assertEquals(Code.C231_CONTINUE, exchange(peer, firstBlock(1)).getCode());
        Thread.sleep(1500);

        assertEquals(Code.C408_REQUEST_ENTITY_INCOMPLETE, exchange(peer, nextBlock(2)).getCode());
    }

    @Test
    public void shouldCompleteSlowUploadThatKeepsSendingBlocks() throws Exception {
        startServer(100, Duration.ofSeconds(2));
        DatagramSocket peer = newPeer();

        assertEquals(Code.C231_CONTINUE, exchange(peer, firstBlock(1)).getCode());
        Thread.sleep(600);
        assertEquals(Code.C231_CONTINUE, exchange(peer, nextBlock(2)).getCode());
        Thread.sleep(600);

        assertEquals(Code.C204_CHANGED, exchange(peer, lastBlock(3)).getCode());
        assertEquals(blockPayload(0, BLOCK_SIZE).concat(blockPayload(1, BLOCK_SIZE)).concat(blockPayload(2, 1)), lastReceivedPayload.get());
    }

    private DatagramSocket newPeer() throws IOException {
        DatagramSocket peer = new DatagramSocket();
        peer.setSoTimeout(3000);
        peers.add(peer);
        return peer;
    }

    private CoapPacket probe(int messageId) {
        return packet(Method.GET, messageId);
    }

    private CoapPacket firstBlock(int messageId) {
        return blockPacket(messageId, 0, true, BLOCK_SIZE);
    }

    private CoapPacket nextBlock(int messageId) {
        return blockPacket(messageId, 1, true, BLOCK_SIZE);
    }

    private CoapPacket lastBlock(int messageId) {
        return blockPacket(messageId, 2, false, 1);
    }

    private CoapPacket blockPacket(int messageId, int blockNr, boolean hasMore, int payloadSize) {
        CoapPacket packet = packet(Method.PUT, messageId);
        packet.headers().setBlock1Req(new BlockOption(blockNr, BlockSize.S_16, hasMore));
        packet.setPayload(blockPayload(blockNr, payloadSize));
        return packet;
    }

    private static Opaque blockPayload(int blockNr, int payloadSize) {
        return opaqueOfSize(blockNr + 1, payloadSize);
    }

    private CoapPacket packet(Method method, int messageId) {
        CoapPacket packet = new CoapPacket(method, MessageType.Confirmable, "/test/1", serverAddress);
        packet.setMessageId(messageId);
        packet.setToken(Opaque.decodeHex("aabb"));
        return packet;
    }

    private CoapPacket exchange(DatagramSocket peer, CoapPacket request) throws Exception {
        byte[] raw = CoapSerializer.serialize(request);
        peer.send(new DatagramPacket(raw, raw.length, serverAddress));

        DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
        peer.receive(reply);
        return CoapSerializer.deserialize(serverAddress, reply.getData(), reply.getLength());
    }
}
