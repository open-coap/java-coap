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
package opencoap.server.observe;

import static opencoap.packet.BlockSize.S_16;
import static opencoap.packet.CoapRequest.get;
import static opencoap.packet.CoapResponse.notFound;
import static opencoap.packet.CoapResponse.ok;
import static opencoap.packet.Opaque.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.Opaque;
import opencoap.packet.SeparateResponse;
import opencoap.utils.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationsReceiverTest {
    private final Service<CoapRequest, CoapResponse> service = mock(Service.class);

    @BeforeEach
    void setUp() {
        reset(service);
    }

    @Test
    void shouldNotifyAndRetrieveBlocks() throws InterruptedException {
        given(service.apply(get("/obs").block2Res(1, S_16, false).build())).willReturn(ok("bbb").toFuture());
        SeparateResponse obs = ok("aaaaaaaaaaaaaaaa").observe(2).block2Res(0, S_16, true).toSeparate(of("100"), null);

        // when
        Opaque payload = NotificationsReceiver.retrieveRemainingBlocks("/obs", obs, service).join();

        // then
        assertEquals(Opaque.of("aaaaaaaaaaaaaaaabbb"), payload);
    }

    @Test
    void shouldFailWhenUnexpectedBlockRetrieving() {
        given(service.apply(get("/obs").block2Res(1, S_16, false).build())).willReturn(notFound().toFuture());
        SeparateResponse obs = ok("aaaaaaaaaaaaaaaa").observe(2).block2Res(0, S_16, true).toSeparate(of("100"), null);

        // when
        CompletableFuture<Opaque> resp = NotificationsReceiver.retrieveRemainingBlocks("/obs", obs, service);

        // then
        assertThrows(CompletionException.class, resp::join);
    }

    @Test
    void doNothingWhenNoBlockResponse() throws InterruptedException {
        SeparateResponse obs = ok("aaaaaaaaaaaaaaaa").observe(2).toSeparate(of("100"), null);

        // when
        Opaque payload = NotificationsReceiver.retrieveRemainingBlocks("/obs", obs, service).join();

        // then
        assertEquals(Opaque.of("aaaaaaaaaaaaaaaa"), payload);
        verifyNoInteractions(service);
    }

}
