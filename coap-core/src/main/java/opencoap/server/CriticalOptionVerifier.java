/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
 * Copyright (C) 2011-2021 ARM Limited. All rights reserved.
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

import static opencoap.packet.CoapResponse.coapResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.Code;
import opencoap.utils.Filter;
import opencoap.utils.Service;

class CriticalOptionVerifier implements Filter.SimpleFilter<CoapRequest, CoapResponse> {

    private final Collection<Integer> recognizedCustomOptions;

    CriticalOptionVerifier() {
        this.recognizedCustomOptions = Collections.emptySet();
    }

    CriticalOptionVerifier(final Collection<Integer> recognizedCustomOptions) {
        this.recognizedCustomOptions = recognizedCustomOptions;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
        if (request.options().containsUnrecognisedCriticalOption(recognizedCustomOptions)) {
            return coapResponse(Code.C402_BAD_OPTION).toFuture();
        }
        return service.apply(request);
    }
}
