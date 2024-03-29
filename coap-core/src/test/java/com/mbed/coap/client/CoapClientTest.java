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
package com.mbed.coap.client;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.observe;
import static com.mbed.coap.packet.CoapRequest.ping;
import static com.mbed.coap.packet.MediaTypes.CT_TEXT_PLAIN;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.reset;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CoapClientTest {
    private CoapClient client;
    private final Service<CoapRequest, CoapResponse> clientService = mock(Service.class);
    private final Opaque token1001 = Opaque.ofBytes(0x03, 0xE9);


    @BeforeEach
    public void setUp() throws Exception {
        reset(clientService);
        client = new CoapClient(LOCAL_5683, clientService, () -> {
        }, CoapClient.defaultResolvePingResponse);
    }

    @Test
    public void request() {
        given(clientService.apply(get("/test").from(LOCAL_5683)))
                .willReturn(CoapResponse.ok("ABC", CT_TEXT_PLAIN).toFuture());

        // when
        CompletableFuture<CoapResponse> resp = client.send(get("/test").from(LOCAL_5683));

        // then
        assertEquals("ABC", resp.join().getPayloadString());
    }

    @Test
    public void pingRequest() throws Exception {
        given(clientService.apply(ping(LOCAL_5683, TransportContext.EMPTY)))
                .willReturn(completedFuture(CoapResponse.of(null)));

        // when
        CompletableFuture<Boolean> resp = client.ping();

        // then
        assertTrue(resp.get());
    }

    @Test
    public void syncRequest() throws CoapException {
        given(clientService.apply(get("/test").from(LOCAL_5683)))
                .willReturn(CoapResponse.ok("ABC", CT_TEXT_PLAIN).toFuture());

        // when
        CoapResponse resp = client.sendSync(get("/test").from(LOCAL_5683));

        // then
        assertEquals("ABC", resp.getPayloadString());
    }


    @Test
    public void observationTest() throws Exception {
        given(clientService.apply(get("/test").token(token1001).observe().from(LOCAL_5683)))
                .willReturn(CoapResponse.ok().payload("1").contentFormat(CT_TEXT_PLAIN).observe(1).toFuture());

        // when
        CompletableFuture<CoapResponse> resp = client.send(observe("/test").token(token1001));

        // then
        assertEquals("1", resp.get().getPayloadString());
    }

}
