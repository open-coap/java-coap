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
package opencoap.endpoint;

import static opencoap.transport.InMemoryCoapTransport.create;
import static opencoap.util.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import java.util.concurrent.ScheduledExecutorService;
import opencoap.core.BlockSize;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Handler;
import org.junit.jupiter.api.Test;
import protocolTests.utils.MockCoapTransport;

public class CoapServerBuilderTest {

    @Test
    public void usingCustomCacheWithoutTransport() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        DefaultDuplicateDetectorCache cache =
                new DefaultDuplicateDetectorCache("testCache", 100, 120_1000, 10_000, 10_000, scheduledExecutorService);
        assertThrows(NullPointerException.class, () ->
                CoapServer.builder().reliability(Reliability.defaults().withDuplicateDetection(DuplicateDetection.using(cache))).build()
        );
    }

    @Test
    public void shouldFail_when_missingTransport() throws Exception {
        assertThrows(NullPointerException.class, () ->
                CoapServer.builder().build()
        );
    }

    @Test
    public void shouldFail_whenBlockSizeAndMaxMessageSizeAreSet() {
        Messaging messaging = Messaging.defaults().withBlockSize(BlockSize.S_512).withMaxMessageSize(2000);

        assertThrows(IllegalArgumentException.class, () ->
                CoapServer.builder().messaging(messaging)
        );
    }

    @Test
    public void shouldFail_whenBertBlockSize() {
        Messaging messaging = Messaging.defaults().withBlockSize(BlockSize.S_1024_BERT);

        assertThrows(IllegalArgumentException.class, () ->
                CoapServer.builder().messaging(messaging)
        );
    }

    @Test
    public void shouldFail_whenModifiedMessagingHasBlockSizeAndMaxMessageSize() {
        CoapServerBuilder builder = CoapServer.builder().messaging(m -> m.withBlockSize(BlockSize.S_512));

        assertThrows(IllegalArgumentException.class, () ->
                builder.messaging(m -> m.withMaxMessageSize(2000))
        );
    }

    @Test
    public void shouldModifyCurrentValues() throws Exception {
        MockCoapTransport transport = new MockCoapTransport();
        MockCoapTransport.MockClient client = transport.client();
        CoapServer server = CoapServer.builder()
                .transport(transport)
                .messaging(m -> m.withBlockSize(BlockSize.S_16))
                .messaging(m -> m.withMaxIncomingBlockTransferSize(20))
                .handler(req -> CoapResponse.ok("0123456789abcdef-0123456789abcdef").toFuture())
                .build().start();

        // then, block size from first modification is kept
        client.send(newCoapPacket(LOCAL_5683).mid(1).con().get().uriPath("/test"));
        client.verifyReceived(newCoapPacket(LOCAL_5683).mid(1).ack(Code.C205_CONTENT).block2Res(0, BlockSize.S_16, true).payload("0123456789abcdef"));

        // and, max incoming transfer size from second modification is applied
        client.send(newCoapPacket(LOCAL_5683).mid(2).con().put().uriPath("/test").block1Req(0, BlockSize.S_16, true).size1(100).payload("0123456789abcdef"));
        assertEquals(Code.C413_REQUEST_ENTITY_TOO_LARGE, client.receive().getCode());

        server.stop();
    }

    @Test
    public void shouldFail_whenNullValues() {
        CoapServerBuilder builder = CoapServer.builder();

        assertThrows(NullPointerException.class, () -> builder.messaging((Messaging) null));
        assertThrows(NullPointerException.class, () -> builder.messaging(m -> null));
        assertThrows(NullPointerException.class, () -> builder.reliability((Reliability) null));
        assertThrows(NullPointerException.class, () -> builder.reliability(r -> null));
        assertThrows(NullPointerException.class, () -> builder.observations(null));
        assertThrows(NullPointerException.class, () -> builder.handler((Handler<CoapRequest, CoapResponse>) null));
        assertThrows(NullPointerException.class, () -> builder.inboundFilter(null));
    }

    @Test
    public void shouldReuseBuilder() throws Exception {
        CoapServer server = new CoapServerBuilder()
                .transport(create(5683))
                .build().start();

        // when, builder is created
        CoapServerBuilder builder = CoapServer.builder();

        // then, it can be reused multiple times
        CoapClient client1 = builder.transport(create()).buildClient(localhost(5683));
        CoapClient client2 = builder.transport(create()).buildClient(localhost(5683));
        assertNotNull(client1.ping().get());
        assertNotNull(client2.ping().get());

        client1.close();
        server.stop();
    }
}
