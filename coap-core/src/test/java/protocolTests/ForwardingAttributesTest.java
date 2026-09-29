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
package protocolTests;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.core.CoapRequest.get;
import static opencoap.core.CoapRequest.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import opencoap.core.AttributeKey;
import opencoap.core.BlockSize;
import opencoap.core.CoapException;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Handler;
import opencoap.core.MessageAttributes;
import opencoap.endpoint.CoapClient;
import opencoap.endpoint.CoapServer;
import opencoap.endpoint.Messaging;
import opencoap.observe.ObserversManager;
import opencoap.routing.RoutingHandler;
import opencoap.transport.InMemoryCoapTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ForwardingAttributesTest {

    private CoapServer server;
    private ObserversManager observersManager = new ObserversManager();
    private final CoapResourceTest coapResourceTest = new CoapResourceTest();
    private final InMemoryCoapTransport srvTransport = spy(new InMemoryCoapTransport(5683));
    private final AttributeKey<String> MY_TEXT = AttributeKey.defaulted("MY_TEXT", "");

    @BeforeEach
    public void setUp() throws IOException {
        server = CoapServer.builder()
                .handler(RoutingHandler.builder()
                        .get("/test", coapResourceTest)
                        .put("/test", coapResourceTest)
                        .get("/obs", observersManager.then(__ -> CoapResponse.ok("A").toFuture()))
                )
                .messaging(Messaging.defaults().withBlockSize(BlockSize.S_16)).transport(srvTransport).build();

        observersManager.init(server);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testRequest() throws IOException, CoapException {
        InMemoryCoapTransport cliTransport = spy(new InMemoryCoapTransport());
        CoapClient client = CoapServer.builder().transport(cliTransport).buildClient(InMemoryCoapTransport.createAddress(5683));

        srvTransport.setAttributes(MessageAttributes.of(MY_TEXT, "dupa"));
        client.sendSync(get("/test").attributes(MessageAttributes.of(MY_TEXT, "client-sending")));
        assertEquals("dupa", coapResourceTest.attributes.get(MY_TEXT));
        verify(cliTransport).sendPacket(argThat(cp ->
                cp.getAttributes().get(MY_TEXT).equals("client-sending")
        ));

        srvTransport.setAttributes(MessageAttributes.of(MY_TEXT, "dupa2"));
        client.sendSync(get("/test"));
        assertEquals("dupa2", coapResourceTest.attributes.get(MY_TEXT));

        client.close();
    }

    @Test
    public void testRequestWithBlocks() throws IOException, CoapException {
        InMemoryCoapTransport cliTransport = spy(new InMemoryCoapTransport());
        CoapClient client = CoapServer.builder().transport(cliTransport).messaging(Messaging.defaults().withBlockSize(BlockSize.S_16)).buildClient(InMemoryCoapTransport.createAddress(5683));

        srvTransport.setAttributes(MessageAttributes.of(MY_TEXT, "dupa"));
        CoapResponse resp = client.sendSync(put("/test").payload("fhdkfhsdkj fhsdjkhfkjsdh fjkhs dkjhfsdjkh")
                .attributes(MessageAttributes.of(MY_TEXT, "client-block")));

        assertEquals(Code.C201_CREATED, resp.getCode());
        assertEquals("dupa", coapResourceTest.attributes.get(MY_TEXT));

        //for each block it sends same transport context
        verify(cliTransport, times(3)).sendPacket(argThat(cp ->
                cp.getAttributes().get(MY_TEXT).equals("client-block")
        ));

        client.close();
    }


    private static class CoapResourceTest implements Handler<CoapRequest, CoapResponse> {

        MessageAttributes attributes;

        @Override
        public CompletableFuture<CoapResponse> apply(CoapRequest req) {
            switch (req.getMethod()) {
                case GET:
                    attributes = req.getAttributes();
                    return completedFuture(CoapResponse.of(Code.C205_CONTENT));

                case PUT:
                    attributes = req.getAttributes();
                    return completedFuture(CoapResponse.of(Code.C201_CREATED));
            }
            throw new IllegalStateException();
        }

    }
}
