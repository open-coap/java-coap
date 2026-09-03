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
package opencoap.server.observe;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import opencoap.packet.CoapRequest;
import opencoap.packet.Opaque;
import opencoap.packet.SeparateResponse;

public final class HashMapObservationsStore implements ObservationsStore {
    private final Map<Opaque, String> obsMap = new ConcurrentHashMap<>();

    @Override
    public void add(CoapRequest obsReq) {
        obsMap.put(obsReq.getToken(), obsReq.options().getUriPath());
    }

    @Override
    public Optional<String> resolveUriPath(SeparateResponse obs) {
        return Optional.ofNullable(obsMap.get(obs.getToken()));
    }

    @Override
    public void remove(SeparateResponse obs) {
        obsMap.remove(obs.getToken());
    }

    public boolean contains(Opaque token) {
        return obsMap.containsKey(token);
    }

    public boolean isEmpty() {
        return obsMap.isEmpty();
    }
}
