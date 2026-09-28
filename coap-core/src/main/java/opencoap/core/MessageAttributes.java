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
package opencoap.core;

import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Immutable per-message attributes that travel with a request or response through the stack but are never serialized:
 * transport facts (e.g. DTLS session data), CoAP-layer hints and application data.
 */
public final class MessageAttributes {

    private final AttributeKey key;
    private final Object value;
    private final MessageAttributes next;

    public static final MessageAttributes EMPTY = new MessageAttributes(null, null, null);
    public static final AttributeKey<Boolean> NON_CONFIRMABLE = AttributeKey.defaulted("NON_CONFIRMABLE", false);
    public static final AttributeKey<Duration> RESPONSE_TIMEOUT = AttributeKey.optional("RESPONSE_TIMEOUT");

    public static <T> MessageAttributes of(AttributeKey<T> key, T value) {
        return new MessageAttributes(requireNonNull(key), requireNonNull(value), null);
    }

    private <T> MessageAttributes(AttributeKey<T> key, T value, MessageAttributes next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }

    /**
     * Returns the value, or the key's default when absent.
     *
     * @throws IllegalStateException when a {@link AttributeKey#required(String) required} key is absent
     */
    public <T> T get(AttributeKey<T> key) {
        T value = get0(requireNonNull(key));
        if (value != null) {
            return value;
        }
        if (key.isRequired()) {
            throw new IllegalStateException("Missing required attribute: " + key);
        }
        return key.getDefaultValue();
    }

    public <T> T getOrDefault(AttributeKey<T> key, T defaultValue) {
        T value = get0(requireNonNull(key));
        return value == null ? defaultValue : value;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private <T> T get0(AttributeKey<T> key) {
        if (this.key == key) {
            return (T) value;
        } else if (next != null) {
            return next.get0(key);
        }
        return null;
    }

    public <T> MessageAttributes with(AttributeKey<T> key, T value) {
        if (this.equals(EMPTY)) {
            return of(key, value);
        }

        return new MessageAttributes(requireNonNull(key), requireNonNull(value), this);
    }

    public MessageAttributes with(MessageAttributes other) {
        if (other.equals(EMPTY)) {
            return this;
        }
        MessageAttributes merged = this.with(other.key, other.value);
        if (other.next == null) {
            return merged;
        }
        return merged.with(other.next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageAttributes that = (MessageAttributes) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, next);
    }

    public Set<AttributeKey<?>> keys() {
        Set<AttributeKey<?>> keys = new HashSet<>();
        for (MessageAttributes attr = this; attr != null; attr = attr.next) {
            if (attr.key != null) {
                keys.add(attr.key);
            }
        }
        return keys;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "MessageAttributes[", "]");
        Set<AttributeKey<?>> seen = new HashSet<>();
        for (MessageAttributes attr = this; attr != null; attr = attr.next) {
            // skip entries shadowed by a later with()
            if (attr.key != null && seen.add(attr.key)) {
                joiner.add(attr.key + "=" + valueToString(attr.value));
            }
        }
        return joiner.toString();
    }

    private static String valueToString(Object value) {
        if (value instanceof byte[]) {
            return Opaque.of((byte[]) value).toHex();
        }
        return String.valueOf(value);
    }
}
