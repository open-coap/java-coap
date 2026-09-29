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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import opencoap.core.BlockSize;
import org.junit.jupiter.api.Test;

class MessagingTest {

    @Test
    void defaults() {
        Messaging messaging = Messaging.defaults();

        assertNull(messaging.getBlockSize());
        assertEquals(1152, messaging.getMaxMessageSize());
        assertFalse(messaging.isMaxMessageSizeSet());
        assertEquals(10_000_000, messaging.getMaxIncomingBlockTransferSize());
        assertEquals(100, messaging.getQueueMaxSize());
        assertTrue(messaging.getRecognizedCustomOptions().isEmpty());
    }

    @Test
    void shouldReturnModifiedCopy() {
        Messaging messaging = Messaging.defaults()
                .withBlockSize(BlockSize.S_256)
                .withMaxMessageSize(2000)
                .withMaxIncomingBlockTransferSize(5000)
                .withQueueMaxSize(10)
                .withRecognizedCustomOptions(Arrays.asList(1000, 1002));

        assertEquals(BlockSize.S_256, messaging.getBlockSize());
        assertEquals(2000, messaging.getMaxMessageSize());
        assertTrue(messaging.isMaxMessageSizeSet());
        assertEquals(5000, messaging.getMaxIncomingBlockTransferSize());
        assertEquals(10, messaging.getQueueMaxSize());
        assertEquals(Arrays.asList(1000, 1002), new ArrayList<>(messaging.getRecognizedCustomOptions()));

        // defaults are not modified
        assertNull(Messaging.defaults().getBlockSize());
        assertFalse(Messaging.defaults().isMaxMessageSizeSet());
    }

    @Test
    void shouldCopyRecognizedCustomOptions() {
        List<Integer> options = new ArrayList<>(Arrays.asList(1000));
        Collection<Integer> recognized = Messaging.defaults().withRecognizedCustomOptions(options).getRecognizedCustomOptions();

        options.add(1002);

        assertEquals(1, recognized.size());
        assertThrows(UnsupportedOperationException.class, () -> recognized.add(1004));
    }

    @Test
    void shouldFail_whenInvalidValues() {
        Messaging messaging = Messaging.defaults();

        assertThrows(IllegalArgumentException.class, () -> messaging.withMaxMessageSize(0));
        assertThrows(IllegalArgumentException.class, () -> messaging.withMaxIncomingBlockTransferSize(0));
        assertThrows(IllegalArgumentException.class, () -> messaging.withQueueMaxSize(0));
        assertThrows(NullPointerException.class, () -> messaging.withRecognizedCustomOptions(null));
    }
}
