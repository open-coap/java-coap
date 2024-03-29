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
package protocolTests;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.put;
import static com.mbed.coap.transmission.RetransmissionBackOff.ofFixed;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.messaging.MessageIdSupplierImpl;
import com.mbed.coap.server.messaging.RequestTagSupplier;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import protocolTests.utils.StubNotificationsReceiver;
import protocolTests.utils.TransportConnectorMock;


public class BlockTest {
    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 5683);
    private TransportConnectorMock transport;
    private CoapClient client;
    private final StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();

    @BeforeEach
    public void setUp() throws Exception {
        transport = new TransportConnectorMock();
        notifReceiver.clear();

        client = CoapServer.builder()
                .transport(transport)
                .midSupplier(new MessageIdSupplierImpl(0))
                .blockSize(BlockSize.S_32)
                .notificationsReceiver(notifReceiver)
                .retransmission(ofFixed(ofMillis(500)))
                .requestTagSupplier(RequestTagSupplier.createSequential(100))
                .buildClient(SERVER_ADDRESS);
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void block2() throws Exception {
        transport.when(newCoapPacket(1).get().uriPath("/path1").build())
                .then(newCoapPacket(1).ack(Code.C205_CONTENT).block2Res(0, BlockSize.S_16, true).payload("123456789012345|").build());
        transport.when(newCoapPacket(2).get().block2Res(1, BlockSize.S_16, false).uriPath("/path1").build())
                .then(newCoapPacket(2).ack(Code.C205_CONTENT).block2Res(1, BlockSize.S_16, false).payload("dupa").build());

        assertEquals("123456789012345|dupa", client.sendSync(get("/path1")).getPayloadString());

    }

    @Test
    public void block2_clientAnticipatesBlockSize() throws Exception {
        transport.when(newCoapPacket(1).get().block2Res(0, BlockSize.S_16, false).uriPath("/path1").build())
                .then(newCoapPacket(1).ack(Code.C205_CONTENT).block2Res(0, BlockSize.S_16, true).payload("123456789012345|").build());
        transport.when(newCoapPacket(2).get().block2Res(1, BlockSize.S_16, false).uriPath("/path1").build())
                .then(newCoapPacket(2).ack(Code.C205_CONTENT).block2Res(1, BlockSize.S_16, false).payload("dupa").build());

        assertEquals("123456789012345|dupa", client.sendSync(get("/path1").blockSize(BlockSize.S_16)).getPayloadString());

    }


    @Test
    public void block1() throws Exception {
        String payload = "123456789012345|123456789012345|dupa";

        transport.when(newCoapPacket(1).put().block1Req(0, BlockSize.S_32, true).size1(payload.length()).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).reqTag("65").payload("123456789012345|123456789012345|").build())
                .then(newCoapPacket(1).ack(Code.C231_CONTINUE).block1Req(0, BlockSize.S_32, true).build());

        transport.when(newCoapPacket(2).put().block1Req(1, BlockSize.S_32, false).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).reqTag("65").payload("dupa").build())
                .then(newCoapPacket(2).ack(Code.C204_CHANGED).block1Req(1, BlockSize.S_32, false).build());

        assertEquals(Code.C204_CHANGED, client.sendSync(put("/path1").payload(payload, MediaTypes.CT_TEXT_PLAIN)).getCode());

    }

    @Test
    public void block1_separateMode() throws Exception {

        String payload = "123456789012345|123456789012345|dupa";

        transport.when(newCoapPacket(1).put().block1Req(0, BlockSize.S_32, true).size1(payload.length()).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).reqTag("65").payload("123456789012345|123456789012345|").build())
                .then(newCoapPacket(1).ack(null).build(),
                        newCoapPacket(2).con(Code.C231_CONTINUE).block1Req(0, BlockSize.S_32, true).build());

        //important that this comes first
        transport.when(newCoapPacket(2).ack(null).build()).thenNothing();

        transport.when(newCoapPacket(2).put().block1Req(1, BlockSize.S_32, false).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).reqTag("65").payload("dupa").build())
                .then(newCoapPacket(2).ack(Code.C204_CHANGED).block1Req(1, BlockSize.S_16, false).build());

        assertEquals(Code.C204_CHANGED, client.sendSync(put("/path1").payload(payload, MediaTypes.CT_TEXT_PLAIN)).getCode());
    }

    @Test
    @Disabled //for backward compatibility with mbed clients
    public void block1_serverChangesBlockSize() throws Exception {

        String payload = "123456789012345|123456789012345|dupa";

        transport.when(newCoapPacket(1).put().block1Req(0, BlockSize.S_32, true).size1(payload.length()).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).payload("123456789012345|123456789012345|").build())
                .then(newCoapPacket(1).ack(Code.C231_CONTINUE).block1Req(0, BlockSize.S_16, true).build());

        // see: https://tools.ietf.org/html/rfc7959#section-2.5
        transport.when(newCoapPacket(2).put().block1Req(2, BlockSize.S_16, false).uriPath("/path1").contFormat(MediaTypes.CT_TEXT_PLAIN).payload("dupa").build())
                .then(newCoapPacket(2).ack(Code.C204_CHANGED).block1Req(2, BlockSize.S_16, false).build());

        assertEquals(Code.C204_CHANGED, client.sendSync(put("/path1").payload(payload, MediaTypes.CT_TEXT_PLAIN)).getCode());

    }
}
