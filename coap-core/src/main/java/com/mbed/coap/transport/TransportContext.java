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
package com.mbed.coap.transport;

import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class TransportContext {

    private final Key key;
    private final Object value;
    private final TransportContext next;

    public static final TransportContext EMPTY = new TransportContext(null, null, null);
    public static final Key<Boolean> NON_CONFIRMABLE = new Key<>(false);
    public static final Key<Duration> RESPONSE_TIMEOUT = new Key<>(null);

    public static <T> TransportContext of(Key<T> key, T value) {
        return new TransportContext(requireNonNull(key), requireNonNull(value), null);
    }

    private <T> TransportContext(Key<T> key, T value, TransportContext next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }

    public <T> T get(Key<T> key) {
        return getOrDefault(key, key.defaultValue);
    }

    public <T> T getOrDefault(Key<T> key, T defaultValue) {
        T value = get0(requireNonNull(key));
        return value == null ? defaultValue : value;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private <T> T get0(Key<T> key) {
        if (this.key == key) {
            return (T) value;
        } else if (next != null) {
            return next.get0(key);
        }
        return null;
    }

    public <T> TransportContext with(Key<T> key, T value) {
        if (this.equals(EMPTY)) {
            return TransportContext.of(key, value);
        }

        return new TransportContext(requireNonNull(key), requireNonNull(value), this);
    }

    public TransportContext with(TransportContext otherCtx) {
        if (otherCtx.equals(EMPTY)) {
            return this;
        }
        TransportContext mergedContext = this.with(otherCtx.key, otherCtx.value);
        if (otherCtx.next == null) {
            return mergedContext;
        }
        return mergedContext.with(otherCtx.next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransportContext that = (TransportContext) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, next);
    }

    public Set<Key<?>> keys() {
        Set<Key<?>> keys = new HashSet<>();
        addKey(keys);
        return keys;
    }

    private void addKey(Set<Key<?>> keys) {
        keys.add(key);
        if (next != null) {
            next.addKey(keys);
        }
    }

    public static final class Key<T> {
        private final T defaultValue;

        public Key(T defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
