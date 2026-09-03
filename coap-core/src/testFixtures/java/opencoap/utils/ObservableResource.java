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
package opencoap.utils;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.packet.CoapResponse.coapResponse;
import java.util.concurrent.CompletableFuture;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.Code;
import opencoap.packet.Opaque;
import opencoap.server.observe.ObserversManager;

public class ObservableResource implements Service<CoapRequest, CoapResponse> {

    private final ObserversManager observersManager;
    private CoapResponse current;
    private final String uriPath;

    public ObservableResource(String uriPath, CoapResponse.Builder current, ObserversManager observersManager) {
        this.current = current.observe(0).build();
        this.observersManager = observersManager;
        this.uriPath = uriPath;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest req) {
        return completedFuture(observersManager.subscribe(req, current));
    }

    public void putPayload(Opaque payload) {
        put(current.withPayload(payload));
    }

    public void terminate(Code code) {
        requireNonNull(code);
        put(coapResponse(code).observe(current.options().getObserve()).build());
    }

    public void put(CoapResponse obs) {
        observersManager.sendObservation(uriPath, __ -> completedFuture(obs));
        current = obs;
    }

    public int observationRelations() {
        return observersManager.size();
    }
}
