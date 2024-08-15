/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.server.messaging;

import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.transport.TransportContext.NON_CONFIRMABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CoapRequestConverterTest {
    private static final TransportContext.Key<Boolean> DUMMY_KEY_IN = new TransportContext.Key<>(false);
    private static final TransportContext.Key<Boolean> DUMMY_KEY_OUT = new TransportContext.Key<>(false);
    private CoapRequestConverter conv = new CoapRequestConverter(() -> 20);
    private Service<CoapRequest, CoapResponse> service = Mockito.mock(Service.class);

    @Test
    void shouldConvertConRequestAndResponse() {
        given(service.apply(eq(
                post("/test2").token(13).payload("test").to(LOCAL_5683))
        )).willReturn(
                ok("ok").toFuture()
        );

        CompletableFuture<CoapPacket> resp = conv.apply(
                newCoapPacket(LOCAL_5683).mid(1300).token(13).post().uriPath("/test2").payload("test").build(), service
        );

        assertEquals(newCoapPacket(LOCAL_5683).mid(1300).token(13).ack(Code.C205_CONTENT).payload("ok").build(), resp.join());
    }

    @Test
    void shouldConvertNonRequestAndResponse() {
        given(service.apply(eq(
                post("/test2").token(13).payload("test").addContext(NON_CONFIRMABLE, true).to(LOCAL_5683))
        )).willReturn(
                ok("ok").toFuture()
        );

        CompletableFuture<CoapPacket> resp = conv.apply(
                newCoapPacket(LOCAL_5683).non().mid(1300).token(13).post().uriPath("/test2").payload("test").build(), service
        );

        assertEquals(newCoapPacket(LOCAL_5683).non(Code.C205_CONTENT).mid(20).token(13).payload("ok").build(), resp.join());
    }

    @Test
    void shouldUseTransportContextFromResponse() {
        given(service.apply(eq(
                post("/test2").token(13).payload("test").addContext(DUMMY_KEY_IN, true).to(LOCAL_5683))
        )).willReturn(
                ok("ok").addContext(DUMMY_KEY_OUT, true).toFuture()
        );

        CompletableFuture<CoapPacket> resp = conv.apply(
                newCoapPacket(LOCAL_5683).mid(1300).token(13).post().uriPath("/test2").payload("test").context(TransportContext.of(DUMMY_KEY_IN, true)).build(), service
        );

        assertEquals(newCoapPacket(LOCAL_5683).ack(Code.C205_CONTENT).mid(1300).token(13).payload("ok").context(TransportContext.of(DUMMY_KEY_IN, true).with(DUMMY_KEY_OUT, true)).build(), resp.join());
    }
}
