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
package opencoap.server;

import static opencoap.packet.CoapRequest.get;
import static opencoap.packet.CoapRequest.observe;
import static opencoap.packet.CoapResponse.ok;
import static opencoap.packet.Opaque.EMPTY;
import static opencoap.packet.Opaque.ofBytes;
import static opencoap.utils.Assertions.assertEquals;
import static opencoap.utils.CoapRequestBuilderFilter.REQUEST_BUILDER_FILTER;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CompletableFuture;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.Opaque;
import opencoap.server.observe.HashMapObservationsStore;
import opencoap.utils.Service;
import org.junit.jupiter.api.Test;

class ObserveRequestFilterTest {

    private HashMapObservationsStore obsMap = new HashMapObservationsStore();
    private ObserveRequestFilter filter = new ObserveRequestFilter(obsMap::add);
    private Service<CoapRequest.Builder, CoapResponse> service = REQUEST_BUILDER_FILTER.andThen(filter).then(req -> ok(req.getToken()).toFuture());

    @Test
    void shouldAddTokenForObservationRequest() {
        CompletableFuture<CoapResponse> resp = service.apply(observe("/obs"));

        assertEquals(ok(ofBytes(1)), resp.join());
    }

    @Test
    void shouldNotChangeTokenForObservationRequestWithExistingToken() {
        CompletableFuture<CoapResponse> resp = service.apply(observe("/obs").token(100));

        assertEquals(ok(ofBytes(100)), resp.join());
    }

    @Test
    void shouldNotChangeTokenForNonObservationRequest() {
        CompletableFuture<CoapResponse> resp = service.apply(get("/obs"));

        assertEquals(ok(EMPTY), resp.join());
    }

    @Test
    void shouldAddObservationRelationForSuccessfulObservationResponse() {
        service = REQUEST_BUILDER_FILTER.andThen(filter).then(req -> ok("ok").observe(12).toFuture());

        CompletableFuture<CoapResponse> resp = service.apply(observe("/obs"));

        assertTrue(obsMap.contains(Opaque.ofBytes(1)));
        assertEquals(ok("ok").observe(12), resp.join());
    }

    @Test
    void shouldNotAddObservationRelationForFailedObservationResponse() {
        service = REQUEST_BUILDER_FILTER.andThen(filter).then(req -> ok("ok").toFuture());

        CompletableFuture<CoapResponse> resp = service.apply(observe("/obs"));

        // then
        assertTrue(obsMap.isEmpty());
        assertEquals(ok("ok"), resp.join());
    }
}
