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
package opencoap.core;

import static java.time.Duration.ofSeconds;
import static opencoap.core.BlockSize.S_32;
import static opencoap.core.BlockSize.S_512;
import static opencoap.core.BlockSize.S_64;
import static opencoap.core.CoapRequest.fetch;
import static opencoap.core.CoapRequest.get;
import static opencoap.core.CoapRequest.ping;
import static opencoap.core.CoapRequest.post;
import static opencoap.core.CoapResponseTest.newOptions;
import static opencoap.core.ContentFormat.APPLICATION_JSON;
import static opencoap.core.Opaque.EMPTY;
import static opencoap.core.Opaque.decodeHex;
import static opencoap.core.TransportContext.NON_CONFIRMABLE;
import static opencoap.core.TransportContext.RESPONSE_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_1_5683;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import java.time.Duration;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoapRequestTest {
    private static final TransportContext.Key<String> DUMMY_KEY = new TransportContext.Key<>(null);

    @Test
    void shouldCreatePing() {
        CoapRequest ping = ping(LOCAL_5683, TransportContext.EMPTY);

        assertTrue(ping.isPing());
        assertThrows(NullPointerException.class, () -> ping.modify().payload(Opaque.of("a")).build().isPing());
        assertThrows(NullPointerException.class, () -> ping.modify().token(decodeHex("12")).build().isPing());

        assertFalse(new CoapRequest(Method.GET, EMPTY, new HeaderOptions(), EMPTY, LOCAL_5683, TransportContext.EMPTY).isPing());
    }

    @Test
    public void shouldModifyCoapRequest() {
        CoapRequest request = new CoapRequest(Method.POST, decodeHex("0102"), newOptions(o -> o.setUriPath("/test")), Opaque.of("test-1"), LOCAL_5683, TransportContext.EMPTY);
        CoapRequest expected = new CoapRequest(Method.POST, decodeHex("ffff"), newOptions(o -> o.setUriPath("/test2")), Opaque.of("test-2"), LOCAL_1_5683, TransportContext.EMPTY);

        // when
        CoapRequest request2 = request.modify()
                .token(decodeHex("ffff"))
                .options(o -> o.uriPath("/test2"))
                .payload(Opaque.of("test-2"))
                .address(LOCAL_1_5683)
                .build();

        // then
        assertEquals(request2, expected);

        // and original object is not changed
        assertEquals(decodeHex("0102"), request.getToken());
        assertEquals(Opaque.of("test-1"), request.getPayload());
        assertEquals(LOCAL_5683, request.getPeerAddress());
    }

    @Test
    void testToString() {
        assertEquals("CoapRequest[PUT URI:/test,Token:03ff, pl(4):64757061]", CoapRequest.put("/test").token(1023).payload("dupa").build().toString());
        assertEquals("CoapRequest[POST URI:/test, pl(4):64757061]", post("/test").payload("dupa").build().toString());
        assertEquals("CoapRequest[DELETE URI:/test,Token:03ff]", CoapRequest.delete("/test").token(1023).build().toString());
        assertEquals("CoapRequest[GET URI:/test]", get("/test").build().toString());
        assertEquals("CoapRequest[FETCH URI:/test, pl(4):64757061]", fetch("/test").payload("dupa").build().toString());
        assertEquals("CoapRequest[PATCH URI:/test, pl(4):64757061]", CoapRequest.patch("/test").payload("dupa").build().toString());
        assertEquals("CoapRequest[IPATCH URI:/test, pl(4):64757061]", CoapRequest.iPatch("/test").payload("dupa").build().toString());
        assertEquals("CoapRequest[GET URI:/test obs:0]", CoapRequest.observe("/test").build().toString());
        assertEquals("CoapRequest[PING]", CoapRequest.ping(LOCAL_5683, TransportContext.EMPTY).toString());
    }

    @Test
    void shouldModifyTransportContext() {
        CoapRequest request = CoapRequest.delete("/test").token(1023).build();

        // when
        CoapRequest request2 = request.modify()
                .addContext(NON_CONFIRMABLE, true)
                .addContext(TransportContext.of(DUMMY_KEY, "test"))
                .build();

        // then
        assertEquals("test", request2.getTransContext(DUMMY_KEY));
        assertEquals(true, request2.getTransContext(NON_CONFIRMABLE));
    }

    @Test
    public void equalsAndHashTest() {
        EqualsVerifier.forClass(CoapRequest.class).suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withPrefabValues(TransportContext.class, TransportContext.EMPTY, TransportContext.of(TransportContext.NON_CONFIRMABLE, true))
                .verify();
    }

    @Nested
    class BuilderTest {

        @Test
        public void buildWithAllPossibleFields() {
            CoapRequest buildRequest = get("/0/1/2").options(o -> o
                            .ifMatch(Opaque.ofBytes(9, 7, 5))
                    )
                    .token(45463L)
                    .accept(APPLICATION_JSON)
                    .maxAge(Duration.ofHours(1))
                    .block1Req(0, S_32, true)
                    .block2Res(0, S_64, true)
                    .etag(decodeHex("010203"))
                    .host("some.com")
                    .queries("p=1")
                    .query("b", "2")
                    .size1(342)
                    .observe()
                    .payload("perse", ContentFormat.TEXT_PLAIN)
                    .addContext(RESPONSE_TIMEOUT, ofSeconds(12))
                    .addContext(DUMMY_KEY, "test")
                    .from(LOCAL_5683);

            CoapRequest expected = new CoapRequest(
                    Method.GET,
                    Opaque.ofBytes(0xB1, 0x97),
                    new HeaderOptions(), Opaque.of("perse"),
                    LOCAL_5683,
                    TransportContext.of(RESPONSE_TIMEOUT, ofSeconds(12)).with(DUMMY_KEY, "test")
            );
            expected.options().setUriPath("/0/1/2");
            expected.options().setAccept(APPLICATION_JSON);
            expected.options().setObserve(0);
            expected.options().setEtag(decodeHex("010203"));
            expected.options().setUriHost("some.com");
            expected.options().setIfMatch(new Opaque[]{Opaque.ofBytes(9, 7, 5)});
            expected.options().setMaxAge(3600L);
            expected.options().setUriQueryList("p=1", "b=2");
            expected.options().setContentFormat(ContentFormat.TEXT_PLAIN);
            expected.options().setBlock1Req(new BlockOption(0, S_32, true));
            expected.options().setBlock2Res(new BlockOption(0, S_64, true));
            expected.options().setSize1(342);

            assertEquals(expected, buildRequest);
        }

        @Test
        public void shouldSetBlockOptionBasedOnMethod() {
            CoapRequest req = fetch("/test").blockSize(S_512).build();
            CoapRequest req2 = get("/test").blockSize(S_512).build();

            assertEquals(new BlockOption(0, S_512, true), req.options().getBlock1Req());
            assertNull(req.options().getBlock2Res());

            assertEquals(new BlockOption(0, S_512, false), req2.options().getBlock2Res());
            assertNull(req2.options().getBlock1Req());
        }

        @Test
        public void shouldSetPayloadFromByteArray() {
            byte[] data = new byte[]{1, 2, 3, 4};

            CoapRequest req = post("/test").payload(data).build();
            assertEquals(Opaque.of(data), req.getPayload());
        }

        @Test
        public void shouldSetPayloadFromByteArrayWithContentFormat() {
            byte[] data = new byte[]{1, 2, 3, 4};

            CoapRequest req = post("/test").payload(data, APPLICATION_JSON).build();
            assertEquals(Opaque.of(data), req.getPayload());
            assertEquals(APPLICATION_JSON, req.options().getContentFormat());
        }

        @Test
        public void shouldSetObserveOption() {
            assertEquals(0, get("/test").observe().build().options().getObserve());
            assertEquals(1, get("/test").deregisterObserve().build().options().getObserve());
        }
    }
}
