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
package opencoap.endpoint.pipeline;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.util.FutureHelpers.failedFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import opencoap.codec.CoapPacket;
import opencoap.core.Filter;
import opencoap.core.Handler;
import opencoap.endpoint.CoapMessageKey;
import opencoap.endpoint.DuplicatedCoapMessageCallback;
import opencoap.endpoint.PutOnlyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateDetector implements Filter<CoapPacket, CoapPacket> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateDetector.class);
    private static final CoapPacket EMPTY_COAP_PACKET = new CoapPacket(null);
    private static final CoapPacket NULL_COAP_PACKET = new CoapPacket(null);

    private final PutOnlyMap<CoapMessageKey, CoapPacket> requestMap;
    private final DuplicatedCoapMessageCallback duplicatedCoapMessageCallback;

    public DuplicateDetector(PutOnlyMap<CoapMessageKey, CoapPacket> cache, DuplicatedCoapMessageCallback duplicatedCoapMessageCallback) {
        this.requestMap = cache;
        this.duplicatedCoapMessageCallback = duplicatedCoapMessageCallback;
    }

    @Override
    public CompletableFuture<CoapPacket> apply(CoapPacket request, Handler<CoapPacket, CoapPacket> service) {
        CoapPacket duplResp = getResponseForRepeatedRequest(request);

        if (duplResp != null) {
            duplicatedCoapMessageCallback.duplicated(request);
            if (duplResp == DuplicateDetector.EMPTY_COAP_PACKET) { // NOPMD
                LOGGER.debug("CoAP request repeated, no response available [{}]", request);
                return failedFuture(new CancellationException());
            } else if (duplResp == DuplicateDetector.NULL_COAP_PACKET) { // NOPMD
                LOGGER.debug("CoAP request repeated, null response available [{}]", request);
                return completedFuture(null);
            } else {
                LOGGER.debug("CoAP request repeated, resending response [{}]", request);
                return completedFuture(duplResp);
            }
        }

        return service.apply(request).thenApply(response -> {
            if (response != null) {
                putResponse(request, response);
            } else {
                putResponse(request, NULL_COAP_PACKET);
            }
            return response;
        });
    }

    private CoapPacket getResponseForRepeatedRequest(CoapPacket request) {
        CoapMessageKey messageKey = new CoapMessageKey(request.getMessageId(), request.getRemoteAddress());

        return requestMap.putIfAbsent(messageKey, EMPTY_COAP_PACKET);
    }

    private void putResponse(CoapPacket request, CoapPacket response) {
        CoapMessageKey messageKey = new CoapMessageKey(request.getMessageId(), request.getRemoteAddress());
        requestMap.put(messageKey, response);
    }

}
