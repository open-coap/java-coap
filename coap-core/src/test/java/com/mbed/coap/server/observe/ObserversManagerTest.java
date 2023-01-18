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
package com.mbed.coap.server.observe;

import static com.mbed.coap.packet.CoapRequest.fetch;
import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapResponse.notFound;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.MediaTypes.CT_APPLICATION_JSON;
import static com.mbed.coap.packet.MediaTypes.CT_APPLICATION_XML;
import static com.mbed.coap.packet.MediaTypes.CT_TEXT_PLAIN;
import static com.mbed.coap.packet.Opaque.variableUInt;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.utils.IpPortAddress;
import com.mbed.coap.utils.Service;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObserversManagerTest {

    private Service<SeparateResponse, Boolean> outboundObservation = mock(Service.class);
    private ObserversManager obsMgr = new ObserversManager();
    private static final InetSocketAddress PEER_1 = IpPortAddress.local(15683).toInetSocketAddress();
    private static final InetSocketAddress PEER_2 = IpPortAddress.local(25683).toInetSocketAddress();

    @BeforeEach
    void setUp() {
        reset(outboundObservation);
        given(outboundObservation.apply(any())).willReturn(completedFuture(true));

        obsMgr.init(outboundObservation);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(outboundObservation);
    }

    @Test
    public void createObservationRelation() {
        assertEquals(0, obsMgr.apply(get("/test").token(13).observe().from(PEER_1), __ -> ok("OK!").toFuture()).join().options().getObserve());
        assertEquals(0, obsMgr.subscribe(get("/test").token(1142).observe().from(PEER_2), ok("OK!")).options().getObserve());
        assertEquals(0, obsMgr.subscribe(fetch("/test2").token(1142).observe().from(PEER_2), ok("OK!")).options().getObserve());

        assertEquals(3, obsMgr.size());
    }

    @Test
    public void doNotCreateObservationRelationWhenMissingObs() throws ExecutionException, InterruptedException {
        // when
        assertNull(obsMgr.subscribe(get("/test").token(13).from(PEER_1), ok("OK!")).options().getObserve());

        // then
        assertEquals(0, obsMgr.size());
    }

    @Test
    public void doNotCreateObservationRelationWhenObsIsNot0() throws ExecutionException, InterruptedException {
        // when
        assertNull(obsMgr.subscribe(get("/test").token(13).observe(10).from(PEER_1), ok("OK!")).options().getObserve());

        // then
        assertEquals(0, obsMgr.size());
    }

    @Test
    public void sendObservation() throws ExecutionException, InterruptedException {
        // given
        obsMgr.subscribe(get("/test").token(13).observe().from(PEER_1), ok("OK!"));
        obsMgr.subscribe(get("/test").token(1312).observe().from(PEER_2), ok("OK!"));

        // when
        obsMgr.sendObservation("/test", __ -> ok("OK 1").toFuture());

        // then
        CoapResponse expected = ok("OK 1").observe(1);
        verify(outboundObservation).apply(eq(expected.toSeparate(variableUInt(13), PEER_1)));
        verify(outboundObservation).apply(eq(expected.toSeparate(variableUInt(1312), PEER_2)));
    }

    @Test
    public void sendObservation_different_for_each_subscriber_accept() {
        // given
        obsMgr.apply(get("/test").token(13).observe().accept(CT_TEXT_PLAIN).from(PEER_1), okResource);
        obsMgr.apply(get("/test").token(1312).observe().accept(CT_APPLICATION_XML).from(PEER_2), okResource);

        // when
        obsMgr.sendObservation("/test", okResource);

        // then
        verify(outboundObservation).apply(eq(ok("OK", CT_TEXT_PLAIN).observe(1).toSeparate(variableUInt(13), PEER_1)));
        verify(outboundObservation).apply(eq(ok("<r>OK</r>", CT_APPLICATION_XML).observe(1).toSeparate(variableUInt(1312), PEER_2)));
    }

    @Test
    public void sendObservation_for_matching_uripath() {
        // given
        obsMgr.subscribe(get("/test").token(101).observe().from(PEER_1), ok("OK!"));
        obsMgr.subscribe(get("/test").token(102).observe().from(PEER_2), ok("OK!"));
        obsMgr.subscribe(get("/test/1").token(103).observe().from(PEER_2), ok("OK!"));
        obsMgr.subscribe(get("/foo").token(104).observe().from(PEER_2), ok("OK!"));

        // when
        obsMgr.sendObservation(it -> it.startsWith("/test"), __ -> ok("OK 1").toFuture());

        // then
        verify(outboundObservation).apply(eq(ok("OK 1").observe(1).toSeparate(variableUInt(101), PEER_1)));
        verify(outboundObservation).apply(eq(ok("OK 1").observe(1).toSeparate(variableUInt(102), PEER_2)));
        verify(outboundObservation).apply(eq(ok("OK 1").observe(2).toSeparate(variableUInt(103), PEER_2)));
    }


    // OK 0d
    @Test
    public void updateSubscription_and_sendObservation() throws ExecutionException, InterruptedException {
        // given
        obsMgr.subscribe(get("/test").token(111).observe().from(PEER_1), ok(""));

        // when
        obsMgr.subscribe(get("/test").token(222).observe().from(PEER_1), ok(""));

        // then
        assertEquals(1, obsMgr.size());

        obsMgr.sendObservation("/test", __ -> completedFuture(ok("OK")));
        CoapResponse expected = ok("OK").observe(1);
        verify(outboundObservation).apply(eq(expected.toSeparate(variableUInt(222), PEER_1)));
    }

    @Test
    public void sendCancelObservation() throws ExecutionException, InterruptedException {
        // given
        obsMgr.subscribe(get("/test").token(13).observe().from(PEER_1), ok("OK!"));

        // when
        obsMgr.sendObservation("/test", __ -> notFound().toFuture());

        // then
        CoapResponse expected = notFound().observe(1);
        verify(outboundObservation).apply(eq(expected.toSeparate(variableUInt(13), PEER_1)));
    }

    @Test
    public void shouldRemoveSubscription_when_gotReset() throws ExecutionException, InterruptedException {
        // given
        obsMgr.subscribe(get("/test").token(13).observe().from(PEER_1), ok("OK!"));
        given(outboundObservation.apply(any())).willReturn(completedFuture(false));

        // when
        obsMgr.sendObservation("/test", __ -> ok("OK").toFuture());

        // then
        assertEquals(0, obsMgr.size());
        verify(outboundObservation).apply(any());
    }

    @Test
    public void deregisterObservationRelation() throws ExecutionException, InterruptedException {
        // given
        obsMgr.subscribe(get("/test").token(13).observe().from(PEER_1), ok("OK!"));
        obsMgr.subscribe(get("/test").token(1312).observe().from(PEER_2), ok("OK!"));
        assertEquals(2, obsMgr.size());

        // when
        assertNull(obsMgr.subscribe(get("/test").token(13).deregisterObserve().from(PEER_1), ok("OK!")).options().getObserve());

        // then
        assertEquals(1, obsMgr.size());
    }

    private static final Service<CoapRequest, CoapResponse> okResource = req -> {
        switch (req.options().getAccept().shortValue()) {
            case CT_TEXT_PLAIN:
                return ok("OK", CT_TEXT_PLAIN).toFuture();
            case CT_APPLICATION_JSON:
                return ok("{\"r\":\"OK\"}", CT_APPLICATION_JSON).toFuture();
            case CT_APPLICATION_XML:
                return ok("<r>OK</r>", CT_APPLICATION_XML).toFuture();
            default:
                return ok("OK").toFuture();
        }
    };

}
