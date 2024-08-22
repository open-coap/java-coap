/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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
package org.opencoap.transport.mbedtls;

import static com.mbed.coap.transport.TransportContext.NON_CONFIRMABLE;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_SESSION_SUSPENSION_HINT;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;

public class DtlsSessionSuspensionService implements Service<CoapRequest, CoapResponse> {
    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request) {
        if (!request.getTransContext(NON_CONFIRMABLE)) {
            return CoapResponse.badRequest().toFuture();
        }

        return CoapResponse.ok().addContext(TransportContext.of(DTLS_SESSION_SUSPENSION_HINT, true)).toFuture();
    }
}
