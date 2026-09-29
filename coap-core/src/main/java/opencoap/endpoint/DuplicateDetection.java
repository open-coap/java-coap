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
import java.util.concurrent.ScheduledExecutorService;
import opencoap.codec.CoapPacket;

/**
 * Detection of duplicated incoming CoAP over UDP messages.
 * <p>
 * Immutable, {@link #onDuplicate(DuplicatedCoapMessageCallback)} returns a modified copy.
 */
public final class DuplicateDetection {
    private static final DuplicateDetection DISABLED = new DuplicateDetection(0, null, DuplicatedCoapMessageCallback.NULL);

    private final int cacheSize;
    private final PutOnlyMap<CoapMessageKey, CoapPacket> cache;
    private final DuplicatedCoapMessageCallback callback;

    private DuplicateDetection(int cacheSize, PutOnlyMap<CoapMessageKey, CoapPacket> cache, DuplicatedCoapMessageCallback callback) {
        this.cacheSize = cacheSize;
        this.cache = cache;
        this.callback = callback;
    }

    /**
     * Detects duplicates with the default in-memory cache, one per built server.
     *
     * @param size maximum number of remembered messages
     * @return duplicate detection
     */
    public static DuplicateDetection cache(int size) {
        require(size > 0, "Duplicate detection cache size must be positive");
        return new DuplicateDetection(size, null, DuplicatedCoapMessageCallback.NULL);
    }

    /**
     * Detects duplicates with the given cache. It is stopped when the server stops.
     *
     * @param cache cache of received messages
     * @return duplicate detection
     */
    public static DuplicateDetection using(PutOnlyMap<CoapMessageKey, CoapPacket> cache) {
        return new DuplicateDetection(0, requireNonNull(cache), DuplicatedCoapMessageCallback.NULL);
    }

    /**
     * Disables duplicate detection, received duplicates are handled as new messages.
     *
     * @return disabled duplicate detection
     */
    public static DuplicateDetection disabled() {
        return DISABLED;
    }

    /**
     * Sets a callback invoked for every detected duplicate.
     *
     * @param callback callback
     * @return modified copy
     */
    public DuplicateDetection onDuplicate(DuplicatedCoapMessageCallback callback) {
        require(isEnabled(), "Duplicate detection is disabled");
        return new DuplicateDetection(cacheSize, cache, requireNonNull(callback));
    }

    boolean isEnabled() {
        return this != DISABLED;
    }

    PutOnlyMap<CoapMessageKey, CoapPacket> createCache(ScheduledExecutorService scheduledExecutorService) {
        if (cache != null) {
            return cache;
        }
        return new DefaultDuplicateDetectorCache("Default cache", cacheSize, scheduledExecutorService);
    }

    DuplicatedCoapMessageCallback getCallback() {
        return callback;
    }
}
