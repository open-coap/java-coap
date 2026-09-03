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
package opencoap.server.messaging;

import static opencoap.utils.Validations.require;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import opencoap.packet.CoapPacket;
import opencoap.packet.MessageType;
import opencoap.utils.Filter;
import opencoap.utils.Service;

public class PiggybackedExchangeFilter implements Filter<CoapPacket, CoapPacket, CoapPacket, Boolean> {

    private final ConcurrentMap<PiggybackedCorrelation, CompletableFuture<CoapPacket>> promises = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<CoapPacket> apply(CoapPacket packet, Service<CoapPacket, Boolean> service) {
        if (packet.getMessageType() == MessageType.NonConfirmable) {
            return service.apply(packet).thenCompose(__ -> new CompletableFuture<>());
        }
        require(packet.getMessageType() == MessageType.Confirmable);

        PiggybackedCorrelation transactionId = new PiggybackedCorrelation(packet);
        CompletableFuture<CoapPacket> newPromise = new CompletableFuture<>();
        CompletableFuture<CoapPacket> prevPromise = promises.putIfAbsent(transactionId, newPromise);
        final CompletableFuture<CoapPacket> promise = prevPromise != null ? prevPromise : newPromise;

        CompletableFuture<CoapPacket> returningPromise = service.apply(packet)
                .thenCompose(__ -> promise);
        returningPromise.whenComplete((__, err) -> promises.remove(transactionId));

        return returningPromise;

    }

    public boolean handleResponse(CoapPacket packet) {
        switch (packet.getMessageType()) {
            case Acknowledgement:
            case Reset:
                CompletableFuture<CoapPacket> promise = promises.remove(new PiggybackedCorrelation(packet));
                if (promise == null) {
                    return false;
                }
                return promise.complete(packet);

            default:
                return false;
        }
    }

    public void stop() {
        promises.forEach((__, promise) -> promise.completeExceptionally(new IOException("Stopped")));
    }

    int transactions() {
        return promises.size();
    }

}
