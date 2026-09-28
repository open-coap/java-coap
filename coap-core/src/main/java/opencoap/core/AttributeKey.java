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

/**
 * Typed key of a {@link MessageAttributes} entry.
 * <p>
 * Keys are compared by identity; the name is a debug label only.
 */
public final class AttributeKey<T> {
    private final String name;
    private final T defaultValue;
    private final boolean required;

    private AttributeKey(String name, T defaultValue, boolean required) {
        this.name = requireNonNull(name);
        this.defaultValue = defaultValue;
        this.required = required;
    }

    /**
     * Key that resolves to the given default value when absent.
     */
    public static <T> AttributeKey<T> defaulted(String name, T defaultValue) {
        return new AttributeKey<>(name, requireNonNull(defaultValue), false);
    }

    /**
     * Key that resolves to null when absent.
     */
    public static <T> AttributeKey<T> optional(String name) {
        return new AttributeKey<>(name, null, false);
    }

    /**
     * Key that must be present; {@link MessageAttributes#get(AttributeKey)} throws {@link IllegalStateException} when absent.
     */
    public static <T> AttributeKey<T> required(String name) {
        return new AttributeKey<>(name, null, true);
    }

    public String getName() {
        return name;
    }

    T getDefaultValue() {
        return defaultValue;
    }

    boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return name;
    }
}
