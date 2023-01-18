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

import static com.mbed.coap.utils.FutureHelpers.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.BlockOption;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.utils.Service;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface NotificationsReceiver {

    NotificationsReceiver REJECT_ALL = (__, ___) -> false;

    /**
     * Handles incoming observation
     *
     * @param resourceUriPath uri-path
     * @param observation coap message
     * @return if false then server will terminate observation by sending RESET
     */
    boolean onObservation(String resourceUriPath, SeparateResponse observation);

    static CompletableFuture<Opaque> retrieveRemainingBlocks(String uriPath, SeparateResponse observation,
            Service<CoapRequest, CoapResponse> outboundService) {

        BlockOption requestBlock2Res = observation.asResponse().options().getBlock2Res();
        if (requestBlock2Res == null || requestBlock2Res.getNr() != 0 || !requestBlock2Res.hasMore()) {
            return completedFuture(observation.getPayload());
        }

        CoapRequest request = CoapRequest.get(uriPath)
                .block2Res(1, observation.options().getBlock2Res().getBlockSize(), false)
                .from(observation.getPeerAddress());

        return outboundService
                .apply(request)
                .thenCompose(resp -> {
                    // merge
                    if (resp.getCode() != Code.C205_CONTENT) {
                        return failedFuture(new CoapCodeException(resp.getCode(), "Unexpected response when retrieving full observation message"));
                    }
                    if (!Objects.equals(observation.options().getEtag(), resp.options().getEtag())) {
                        return failedFuture(new CoapException("Could not retrieve full observation message, etag does not mach"));
                    }

                    Opaque newPayload = observation.getPayload().concat(resp.getPayload());
                    return completedFuture(newPayload);
                });
    }

}
