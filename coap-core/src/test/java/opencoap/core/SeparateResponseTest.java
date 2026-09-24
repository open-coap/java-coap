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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static opencoap.core.CoapResponse.coapResponse;
import static opencoap.core.Code.C205_CONTENT;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Func;
import org.junit.jupiter.api.Test;

class SeparateResponseTest {
    private static final TransportContext.Key<String> DUMMY_KEY = new TransportContext.Key<>(null);

    @Test
    public void shouldModifyWithBuilder() {
        SeparateResponse response = coapResponse(C205_CONTENT).payload("moi").maxAge(100).toSeparate(Opaque.of("token"), LOCAL_5683);

        // when
        SeparateResponse response2 = response.modify()
                .payload("czesc")
                .options(o -> o.maxAge(200))
                .addContext(DUMMY_KEY, "test")
                .build();

        // then
        assertEquals(
                coapResponse(C205_CONTENT).payload("czesc").maxAge(200).addContext(DUMMY_KEY, "test").toSeparate(Opaque.of("token"), LOCAL_5683),
                response2
        );

        // and original object is not changed
        assertEquals("moi", response.getPayload().toUtf8String());
        assertEquals(100, response.options().getMaxAge());
    }

    @Test
    public void equalsAndHashTest() {
        EqualsVerifier.forClass(SeparateResponse.class)
                .withGenericPrefabValues(Supplier.class, (Func.Func1<CompletableFuture<CoapResponse>, Supplier>) o -> () -> o)
                .withGenericPrefabValues(CompletableFuture.class, (Func.Func1<CoapResponse, CompletableFuture>) coapResponse -> new CompletableFuture<>())
                .withPrefabValues(CoapResponse.class, CoapResponse.badRequest().build(), CoapResponse.ok().build())
                .withPrefabValues(TransportContext.class, TransportContext.EMPTY, TransportContext.of(TransportContext.NON_CONFIRMABLE, true))
                .usingGetClass()
                .verify();
    }
}
