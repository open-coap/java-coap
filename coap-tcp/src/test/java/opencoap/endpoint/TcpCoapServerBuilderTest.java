/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package opencoap.endpoint;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Handler;
import opencoap.routing.RoutingHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import protocolTests.utils.MockCoapTcpTransport;
import protocolTests.utils.MockCoapTransport;

class TcpCoapServerBuilderTest {
    private final MockCoapTcpTransport transport = new MockCoapTcpTransport();
    private final MockCoapTransport.MockClient client = transport.client();
    private CoapServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldApplyInboundFilter() throws Exception {
        server = TcpCoapServer.builder()
                .transport(transport)
                .inboundFilter((req, next) -> req.options().getUriPath().equals("/forbidden")
                        ? CoapResponse.coapResponse(Code.C403_FORBIDDEN).toFuture()
                        : next.apply(req)
                )
                .handler(RoutingHandler.builder()
                        .get("/*", req -> CoapResponse.ok("ok").toFuture())
                )
                .build()
                .start();

        client.send(newCoapPacket(LOCAL_5683).mid(1).get().uriPath("/forbidden"));
        client.verifyReceived(newCoapPacket(LOCAL_5683).mid(1).ack(Code.C403_FORBIDDEN));

        client.send(newCoapPacket(LOCAL_5683).mid(2).get().uriPath("/test"));
        client.verifyReceived(newCoapPacket(LOCAL_5683).mid(2).ack(Code.C205_CONTENT).payload("ok"));
    }

    @Test
    void shouldFail_whenNullValues() {
        TcpCoapServerBuilder builder = TcpCoapServer.builder();

        assertThrows(NullPointerException.class, () -> builder.messaging((Messaging) null));
        assertThrows(NullPointerException.class, () -> builder.messaging(m -> null));
        assertThrows(NullPointerException.class, () -> builder.observations(null));
        assertThrows(NullPointerException.class, () -> builder.handler((Handler<CoapRequest, CoapResponse>) null));
        assertThrows(NullPointerException.class, () -> builder.inboundFilter(null));
        assertThrows(NullPointerException.class, () -> builder.csmStorage(null));
    }
}
