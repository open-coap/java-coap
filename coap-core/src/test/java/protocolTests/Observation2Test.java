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

import static com.mbed.coap.packet.CoapRequest.observe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.messaging.MessageIdSupplierImpl;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocolTests.utils.StubNotificationsReceiver;
import protocolTests.utils.TransportConnectorMock;


public class Observation2Test {


    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 5683);
    private TransportConnectorMock transport;
    private CoapClient client;
    private final StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();

    @BeforeEach
    public void setUp() throws Exception {
        notifReceiver.clear();

        transport = new TransportConnectorMock();

        client = CoapServer.builder().transport(transport).midSupplier(new MessageIdSupplierImpl(0))
                .notificationsReceiver(notifReceiver)
                .buildClient(SERVER_ADDRESS);

        //establish observation relation
        transport.when(newCoapPacket(1).get().uriPath("/path1").obs(0).token(1).build())
                .then(newCoapPacket(1).ack(Code.C205_CONTENT).obs(0).token(1).payload("12345").build());

        assertEquals("12345", client.sendSync(observe("/path1").token(1)).getPayloadString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void shouldReceiveEmptyAckAfterObservation() throws Exception {
        //send observation
        transport.receive(newCoapPacket(SERVER_ADDRESS).mid(3).con(Code.C205_CONTENT).obs(2).token(1).payload("perse perse").build());

        //important, no token included in response
        assertEquals(transport.getLastOutgoingMessage(), newCoapPacket(SERVER_ADDRESS).mid(3).ack(null).build());
        assertEquals(notifReceiver.take().asResponse().getPayloadString(), "perse perse");
    }
}
