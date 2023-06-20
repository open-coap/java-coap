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
package com.mbed.coap.server;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class ObserveRequestFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private final AtomicLong nextToken = new AtomicLong(0);
    private final Consumer<CoapRequest> registerRelation;
    private final static Integer INIT_OBSERVE = 0;

    ObserveRequestFilter(Consumer<CoapRequest> registerRelation) {
        this.registerRelation = registerRelation;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest req, Service<CoapRequest, CoapResponse> service) {
        if (!INIT_OBSERVE.equals(req.options().getObserve())) {
            return service.apply(req);
        }

        CoapRequest obsReq;
        if (req.getToken().isEmpty()) {
            obsReq = req.withToken(Opaque.variableUInt(nextToken.incrementAndGet()));
        } else {
            obsReq = req;
        }

        return service.apply(obsReq)
                .thenApply(resp -> {
                            if (isSuccessfulObservationSubscription(resp)) {
                                registerRelation.accept(obsReq);
                            }
                            return resp;
                        }
                );
    }

    private static boolean isSuccessfulObservationSubscription(CoapResponse resp) {
        return resp.options().getObserve() != null;
    }
}
