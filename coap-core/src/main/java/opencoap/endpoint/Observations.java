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

import static java.util.Objects.requireNonNull;
import static opencoap.util.Validations.require;
import java.util.function.Supplier;
import opencoap.observe.NotificationsReceiver;
import opencoap.observe.ObservationsStore;

/**
 * Client side handling of observations: receiving notifications for observation requests sent by the server.
 * <p>
 * Immutable, {@link #withStore(ObservationsStore)} returns a modified copy.
 */
public final class Observations {
    private static final Observations NONE = new Observations(NotificationsReceiver.REJECT_ALL, () -> ObservationsStore.ALWAYS_EMPTY);

    private final NotificationsReceiver receiver;
    private final Supplier<ObservationsStore> storeSupplier;

    private Observations(NotificationsReceiver receiver, Supplier<ObservationsStore> storeSupplier) {
        this.receiver = receiver;
        this.storeSupplier = storeSupplier;
    }

    /**
     * Observations are not tracked, all notifications are rejected (default).
     *
     * @return observations
     */
    public static Observations none() {
        return NONE;
    }

    /**
     * Passes notifications to the receiver. Observation relations are kept in an in-memory store, one per built
     * server, unless {@link #withStore(ObservationsStore)} is set.
     *
     * @param receiver notifications receiver
     * @return observations
     */
    public static Observations receiving(NotificationsReceiver receiver) {
        return new Observations(requireNonNull(receiver), ObservationsStore::inMemory);
    }

    /**
     * Sets custom store for observation relations, for example one that uses external storage.
     *
     * @param store observations store
     * @return modified copy
     */
    public Observations withStore(ObservationsStore store) {
        requireNonNull(store);
        require(this != NONE, "Observations store requires a notifications receiver");
        return new Observations(receiver, () -> store);
    }

    public NotificationsReceiver getReceiver() {
        return receiver;
    }

    /**
     * Returns the observations store for a server that is being built.
     *
     * @return configured store, or a new in-memory one
     */
    public ObservationsStore createStore() {
        return storeSupplier.get();
    }
}
