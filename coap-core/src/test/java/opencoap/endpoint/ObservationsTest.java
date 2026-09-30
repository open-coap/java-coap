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
package opencoap.endpoint;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import opencoap.observe.HashMapObservationsStore;
import opencoap.observe.NotificationsReceiver;
import opencoap.observe.ObservationsStore;
import org.junit.jupiter.api.Test;

class ObservationsTest {
    private final NotificationsReceiver receiver = (uriPath, observation) -> true;

    @Test
    void none() {
        assertSame(NotificationsReceiver.REJECT_ALL, Observations.none().getReceiver());
        assertSame(ObservationsStore.ALWAYS_EMPTY, Observations.none().createStore());
    }

    @Test
    void shouldCreateNewInMemoryStore_forEachServer() {
        Observations observations = Observations.receiving(receiver);

        assertSame(receiver, observations.getReceiver());
        assertInstanceOf(HashMapObservationsStore.class, observations.createStore());
        assertNotSame(observations.createStore(), observations.createStore());
    }

    @Test
    void shouldUseGivenStore() {
        ObservationsStore store = ObservationsStore.inMemory();
        Observations observations = Observations.receiving(receiver).withStore(store);

        assertSame(receiver, observations.getReceiver());
        assertSame(store, observations.createStore());
    }

    @Test
    void shouldFail_whenInvalidValues() {
        assertThrows(NullPointerException.class, () -> Observations.receiving(null));
        assertThrows(NullPointerException.class, () -> Observations.receiving(receiver).withStore(null));
        assertThrows(IllegalArgumentException.class, () -> Observations.none().withStore(ObservationsStore.inMemory()));
    }
}
