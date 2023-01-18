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

import static com.mbed.coap.utils.Validations.require;
import static java.util.Objects.requireNonNull;
import com.mbed.coap.packet.CoapOptionsBuilder;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Method;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObserversManager implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ObserversManager.class);
    private volatile Service<SeparateResponse, Boolean> outboundObservation;
    //               uri-path,    address,           subscribing request
    private final Map<String, Map<InetSocketAddress, CoapRequest>> obsRelations = new ConcurrentHashMap<>();
    private final AtomicInteger observeSeq = new AtomicInteger(0);

    public void init(Service<SeparateResponse, Boolean> outboundObservation) {
        obsRelations.clear();
        this.outboundObservation = requireNonNull(outboundObservation);
    }

    public void init(CoapServer server) {
        require(!server.isRunning(), "SubscriptionManager should be initialized with non yet running server");
        init(server.outboundResponseService());
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
        return service.apply(request)
                .thenApply(resp -> subscribe(request, resp));
    }

    public CoapResponse subscribe(CoapRequest req, CoapResponse resp) {
        if (req.getMethod() != Method.GET && req.getMethod() != Method.FETCH) {
            return resp;
        }

        if (resp.getCode() == Code.C205_CONTENT && Objects.equals(0, req.options().getObserve())) {
            putFrom(req);
            return updateObserve(resp);
        } else if (Objects.equals(1, req.options().getObserve())) {
            remove(req.options().getUriPath(), req.getPeerAddress());
        }

        return resp.options(CoapOptionsBuilder::unsetObserve);
    }

    private CoapResponse updateObserve(CoapResponse resp) {
        if (resp.options().getObserve() != null) {
            return resp;
        }
        return resp.options(o -> o.observe(observeSeq.get()));
    }

    public void sendObservation(String uriPath, Service<CoapRequest, CoapResponse> service) {
        Map<InetSocketAddress, CoapRequest> subscriptions = obsRelations.getOrDefault(uriPath, Collections.emptyMap());
        if (subscriptions.isEmpty()) {
            return;
        }

        int currentObserveSequence = observeSeq.incrementAndGet();
        for (InetSocketAddress peerAddress : subscriptions.keySet()) {
            CoapRequest subscribeRequest = subscriptions.get(peerAddress);

            service.apply(subscribeRequest)
                    .thenApply(obsResponse ->
                            toSeparateResponse(obsResponse, currentObserveSequence, subscribeRequest)
                    )
                    .thenAccept(separateResponse ->
                            sendObservation(uriPath, separateResponse)
                    );

        }
    }

    public void sendObservation(Predicate<String> uriPathFilter, Service<CoapRequest, CoapResponse> service) {
        obsRelations.keySet().stream()
                .filter(uriPathFilter)
                .forEach(uriPath -> sendObservation(uriPath, service));
    }

    private void sendObservation(String uriPath, SeparateResponse separateResponse) {
        InetSocketAddress peerAddress = separateResponse.getPeerAddress();

        outboundObservation.apply(separateResponse).whenComplete((result, exception) -> {
            if (exception != null) {
                remove(uriPath, peerAddress);
                LOGGER.warn("[{}#{}] Removed observation relation, got exception: {}", peerAddress, separateResponse.getToken(), exception.toString());
            } else if (!result) {
                remove(uriPath, peerAddress);
                LOGGER.info("[{}#{}] Removed observation relation, got reset", peerAddress, separateResponse.getToken());
            }
        });
        if (separateResponse.getCode() != Code.C205_CONTENT) {
            obsRelations.clear();
        }
    }

    private static SeparateResponse toSeparateResponse(CoapResponse obsResponse, int currentObserveSequence, CoapRequest subscribeRequest) {
        return obsResponse.options(o -> o.observe(currentObserveSequence))
                .toSeparate(subscribeRequest.getToken(), subscribeRequest.getPeerAddress());
    }

    private void putFrom(CoapRequest coapRequest) {
        String uriPath = coapRequest.options().getUriPath();
        InetSocketAddress peerAddress = coapRequest.getPeerAddress();

        obsRelations.compute(uriPath, (__, relations) -> {
                    if (relations == null) {
                        relations = new HashMap<>();
                    }
                    relations.put(peerAddress, coapRequest);
                    return relations;
                }
        );
    }

    private void remove(String uriPath, InetSocketAddress peerAddress) {
        obsRelations.computeIfPresent(uriPath, (__, entry) -> {
            entry.remove(peerAddress);
            if (entry.isEmpty()) {
                return null;
            }
            return entry;
        });
    }

    public int size() {
        return obsRelations.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

}
