/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.packet;

import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.packet.Code.C201_CREATED;
import static com.mbed.coap.packet.Code.C204_CHANGED;
import static com.mbed.coap.packet.Code.C205_CONTENT;
import static com.mbed.coap.packet.Code.C400_BAD_REQUEST;
import static com.mbed.coap.packet.Code.C404_NOT_FOUND;
import static com.mbed.coap.packet.MediaTypes.CT_APPLICATION_JSON;
import static com.mbed.coap.packet.MediaTypes.CT_TEXT_PLAIN;
import static com.mbed.coap.packet.Opaque.decodeHex;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import com.mbed.coap.transport.TransportContext;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Func;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


class CoapResponseTest {
    private static final TransportContext.Key<String> DUMMY_KEY = new TransportContext.Key<>(null);
    private static final TransportContext.Key<String> DUMMY_KEY2 = new TransportContext.Key<>(null);

    @Test
    void staticFactory() {
        assertEquals(CoapResponse.ok(), CoapResponse.of(C205_CONTENT, Opaque.EMPTY));
        assertEquals(CoapResponse.ok(Opaque.of("test")), CoapResponse.of(C205_CONTENT, "test"));
        assertEquals(CoapResponse.ok("test"), CoapResponse.of(C205_CONTENT, Opaque.of("test")));
        assertEquals(CoapResponse.ok("test"), CoapResponse.of(C205_CONTENT, Opaque.of("test")));
        assertEquals(CoapResponse.badRequest(), CoapResponse.of(C400_BAD_REQUEST, Opaque.EMPTY));
        assertEquals(CoapResponse.notFound(), CoapResponse.of(C404_NOT_FOUND, Opaque.EMPTY));
    }

    @Test
    public void shouldModifyPayload() {
        CoapResponse response = CoapResponse.of(C205_CONTENT, Opaque.of("moi"));
        CoapResponse expected = CoapResponse.of(C205_CONTENT, Opaque.of("czesc"));

        assertEquals(expected, response.withPayload(Opaque.of("czesc")));
        assertEquals("moi", response.getPayloadString());
    }

    @Test
    public void shouldModifyOptions() {
        CoapResponse response = CoapResponse.of(C205_CONTENT, Opaque.of("test"), newOptions(o -> o.setMaxAge(100L)));
        CoapResponse expected = CoapResponse.of(C205_CONTENT, Opaque.of("test"), newOptions(o -> o.setMaxAge(200L)));

        assertEquals(expected, response.withOptions(o -> o.maxAge(200)));
        assertEquals(100, response.options().getMaxAge());
    }

    @Test
    public void equalsAndHashTest() {
        EqualsVerifier.forClass(CoapResponse.class)
                .withGenericPrefabValues(Supplier.class, (Func.Func1<CompletableFuture<CoapResponse>, Supplier>) o -> () -> o)
                .withGenericPrefabValues(CompletableFuture.class, (Func.Func1<CoapResponse, CompletableFuture>) coapResponse -> new CompletableFuture<>())
                .withPrefabValues(CoapResponse.class, CoapResponse.badRequest().build(), CoapResponse.ok().build())
                .withPrefabValues(TransportContext.class, TransportContext.EMPTY, TransportContext.of(TransportContext.NON_CONFIRMABLE, true))
                .usingGetClass()
                .verify();
    }

    @Test
    void testToString() {
        assertEquals("CoapResponse[205, pl(4):64757061]", CoapResponse.ok("dupa").build().toString());
        assertEquals("CoapResponse[400, ETag:6565]", CoapResponse.badRequest().etag(Opaque.of("ee")).build().toString());
        assertEquals("CoapResponse[205, ContTp:0, pl(3):616161]", CoapResponse.ok("aaa", CT_TEXT_PLAIN).build().toString());
        assertEquals("CoapResponse[400, ContTp:50, pl(13):7b226572726f72223a3132337d]", coapResponse(C400_BAD_REQUEST).payload("{\"error\":123}").contentFormat(CT_APPLICATION_JSON).build().toString());
    }

    @Test
    void shouldAccessTransportContext() {
        // when
        CoapResponse response = CoapResponse.ok()
                .context(TransportContext.EMPTY)
                .addContext(DUMMY_KEY, "test")
                .addContext(TransportContext.of(DUMMY_KEY2, "test2"))
                .build();

        // then
        assertEquals("test", response.getTransContext(DUMMY_KEY));
        assertEquals("test2", response.getTransContext(DUMMY_KEY2));
    }

    @Nested
    class BuilderTest {

        @Test
        public void shouldBuildComplex() {
            CoapResponse response = coapResponse(C201_CREATED)
                    .payload(Opaque.of("{'test:1}"))
                    .contentFormat(CT_APPLICATION_JSON)
                    .observe(123)
                    .etag(decodeHex("0102"))
                    .size2Res(9)
                    .locationPath("/test")
                    .maxAge(Duration.ofMinutes(1))
                    .options(o -> o
                            .proxyUri("/proxy/test")
                            .ifMatch(decodeHex("99"))
                            .size1(432)
                    )
                    .build();

            CoapResponse expected = CoapResponse.of(C201_CREATED, Opaque.of("{'test:1}"), newOptions(o -> {
                o.setContentFormat(CT_APPLICATION_JSON);
                o.setObserve(123);
                o.setEtag(decodeHex("0102"));
                o.setSize2Res(9);
                o.setLocationPath("/test");
                o.setMaxAge(60L);
                o.setProxyUri("/proxy/test");
                o.setIfMatch(new Opaque[]{decodeHex("99")});
                o.setSize1(432);
            }));

            assertEquals(expected, response);
        }


        @Test
        public void shouldReturnCompletedFuture() throws ExecutionException, InterruptedException {
            CompletableFuture<CoapResponse> future = coapResponse(C205_CONTENT).toFuture();

            assertTrue(future.isDone());
            assertEquals(future.get(), CoapResponse.of(C205_CONTENT));
        }

        @Test
        public void shouldReturnSeparateResponse() {
            SeparateResponse separateResponse = coapResponse(C204_CHANGED).toSeparate(decodeHex("0102"), LOCAL_5683);
            SeparateResponse expected = new SeparateResponse(CoapResponse.of(C204_CHANGED), decodeHex("0102"), LOCAL_5683, TransportContext.EMPTY);

            assertEquals(expected, separateResponse);
        }
    }

    static HeaderOptions newOptions(Consumer<HeaderOptions> optionsFunc) {
        HeaderOptions options = new HeaderOptions();
        optionsFunc.accept(options);
        return options;
    }

}
