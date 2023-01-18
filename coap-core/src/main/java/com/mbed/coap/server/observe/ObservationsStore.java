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

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.SeparateResponse;
import java.util.Optional;

public interface ObservationsStore {

    void add(CoapRequest obsReq);

    Optional<String> resolveUriPath(SeparateResponse obs);

    void remove(SeparateResponse obs);

    static ObservationsStore inMemory() {
        return new HashMapObservationsStore();
    }

    ObservationsStore ALWAYS_EMPTY = new ObservationsStore() {
        @Override
        public void add(CoapRequest obsReq) {
        }

        @Override
        public Optional<String> resolveUriPath(SeparateResponse obs) {
            return Optional.empty();
        }

        @Override
        public void remove(SeparateResponse obs) {
        }
    };
}
