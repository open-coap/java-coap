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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.util.concurrent.ScheduledExecutorService;
import opencoap.codec.CoapPacket;
import org.junit.jupiter.api.Test;

class DuplicateDetectionTest {
    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    @Test
    void shouldCreateNewDefaultCache_forEachServer() {
        DuplicateDetection duplicateDetection = DuplicateDetection.cache(100);

        assertTrue(duplicateDetection.isEnabled());
        assertInstanceOf(DefaultDuplicateDetectorCache.class, duplicateDetection.createCache(executor));
        assertNotSame(duplicateDetection.createCache(executor), duplicateDetection.createCache(executor));
    }

    @Test
    void shouldUseGivenCache() {
        PutOnlyMap<CoapMessageKey, CoapPacket> cache = new DefaultDuplicateDetectorCache("test", 100, executor);
        DuplicateDetection duplicateDetection = DuplicateDetection.using(cache);

        assertTrue(duplicateDetection.isEnabled());
        assertSame(cache, duplicateDetection.createCache(executor));
    }

    @Test
    void shouldSetCallback() {
        DuplicatedCoapMessageCallback callback = packet -> {
        };

        assertSame(callback, DuplicateDetection.cache(100).onDuplicate(callback).getCallback());
        assertSame(DuplicatedCoapMessageCallback.NULL, DuplicateDetection.cache(100).getCallback());
    }

    @Test
    void disabled() {
        assertFalse(DuplicateDetection.disabled().isEnabled());
        assertThrows(IllegalArgumentException.class, () -> DuplicateDetection.disabled().onDuplicate(packet -> {
        }));
    }

    @Test
    void shouldFail_whenInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> DuplicateDetection.cache(0));
        assertThrows(IllegalArgumentException.class, () -> DuplicateDetection.cache(-1));
        assertThrows(NullPointerException.class, () -> DuplicateDetection.using(null));
        assertThrows(NullPointerException.class, () -> DuplicateDetection.cache(100).onDuplicate(null));
    }
}
