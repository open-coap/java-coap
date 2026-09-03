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
package opencoap.transport.mbedtls;

import static opencoap.transport.TransportContext.NON_CONFIRMABLE;
import static opencoap.transport.mbedtls.DtlsTransportContext.DTLS_SESSION_SUSPENSION_HINT;
import java.util.concurrent.CompletableFuture;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.transport.TransportContext;
import opencoap.utils.Service;

public class DtlsSessionSuspensionService implements Service<CoapRequest, CoapResponse> {
    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request) {
        if (!request.getTransContext(NON_CONFIRMABLE)) {
            return CoapResponse.badRequest().toFuture();
        }

        return CoapResponse.ok().addContext(TransportContext.of(DTLS_SESSION_SUSPENSION_HINT, true)).toFuture();
    }
}
