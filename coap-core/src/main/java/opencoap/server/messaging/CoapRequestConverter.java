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

import java.util.concurrent.CompletableFuture;
import opencoap.packet.CoapPacket;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.MessageType;
import opencoap.utils.Filter;
import opencoap.utils.Service;

public class CoapRequestConverter implements Filter<CoapPacket, CoapPacket, CoapRequest, CoapResponse> {

    private final MessageIdSupplier midSupplier;

    public CoapRequestConverter(MessageIdSupplier midSupplier) {
        this.midSupplier = midSupplier;
    }

    @Override
    public CompletableFuture<CoapPacket> apply(CoapPacket packet, Service<CoapRequest, CoapResponse> service) {
        return service
                .apply(packet.toCoapRequest())
                .thenApply(coapResponse -> {
                    CoapPacket responsePacket = packet.createResponseFrom(coapResponse);

                    if (responsePacket.getMessageType() == MessageType.NonConfirmable) {
                        //Non-confirmable messages must also use mid for deduplication purpose
                        midSupplier.update(responsePacket);
                    }
                    return responsePacket;
                });
    }
}
