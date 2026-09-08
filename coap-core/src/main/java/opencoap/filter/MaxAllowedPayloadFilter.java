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
package opencoap.filter;

import static opencoap.core.CoapResponse.coapResponse;
import java.util.concurrent.CompletableFuture;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Filter;
import opencoap.core.Service;

public class MaxAllowedPayloadFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private final int max;
    private final String msg;

    public MaxAllowedPayloadFilter(int max, String msg) {
        this.max = max;
        this.msg = msg;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {

        if (request.getPayload().size() > max) {
            return coapResponse(Code.C413_REQUEST_ENTITY_TOO_LARGE)
                    .options(o -> o.size1(max))
                    .payload(msg)
                    .toFuture();
        } else {
            return service.apply(request);
        }
    }
}
