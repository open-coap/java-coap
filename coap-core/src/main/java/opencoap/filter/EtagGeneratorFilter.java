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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import opencoap.core.CoapOptions;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Filter;
import opencoap.core.Opaque;
import opencoap.core.Service;

public final class EtagGeneratorFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {

    private final Function<Opaque, Opaque> etagGenerator;

    public static final EtagGeneratorFilter PAYLOAD_HASHING = new EtagGeneratorFilter(payload -> Opaque.variableUInt(Arrays.hashCode(payload.getBytes())));

    public EtagGeneratorFilter(Function<Opaque, Opaque> etagGenerator) {
        this.etagGenerator = Objects.requireNonNull(etagGenerator);
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
        return service
                .apply(request)
                .thenApply(this::updateEtag);
    }

    private CoapResponse updateEtag(CoapResponse resp) {
        return resp.withOptions(o ->
                o.ifNull(CoapOptions::getEtagArray, __ ->
                        o.etag(etagGenerator.apply(resp.getPayload()))
                )
        );
    }
}
