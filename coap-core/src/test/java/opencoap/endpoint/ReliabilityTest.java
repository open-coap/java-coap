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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReliabilityTest {

    @Test
    void defaults() {
        Reliability reliability = Reliability.defaults();

        assertEquals(Duration.ofMinutes(2), reliability.getResponseTimeout());
        assertNotNull(reliability.getRetransmission());
        assertSame(DuplicatedCoapMessageCallback.NULL, reliability.getDuplicateDetection().getCallback());
    }

    @Test
    void shouldReturnModifiedCopy() {
        RetransmissionBackOff retransmission = RetransmissionBackOff.ofFixed(Duration.ofMillis(500));
        MessageIdSupplier messageIdSupplier = MessageIdSupplier.sequential(0);
        RequestTagSupplier requestTagSupplier = RequestTagSupplier.sequential(100);
        DuplicateDetection duplicateDetection = DuplicateDetection.disabled();

        Reliability reliability = Reliability.defaults()
                .withRetransmission(retransmission)
                .withResponseTimeout(Duration.ofSeconds(10))
                .withMessageIdSupplier(messageIdSupplier)
                .withRequestTagSupplier(requestTagSupplier)
                .withDuplicateDetection(duplicateDetection);

        assertSame(retransmission, reliability.getRetransmission());
        assertEquals(Duration.ofSeconds(10), reliability.getResponseTimeout());
        assertSame(messageIdSupplier, reliability.resolveMessageIdSupplier());
        assertSame(requestTagSupplier, reliability.resolveRequestTagSupplier());
        assertSame(duplicateDetection, reliability.getDuplicateDetection());

        // defaults are not modified
        assertEquals(Duration.ofMinutes(2), Reliability.defaults().getResponseTimeout());
    }

    @Test
    void shouldSetFixedRetransmission() {
        RetransmissionBackOff noRetransmission = Reliability.defaults().withFixedRetransmission(Duration.ofMillis(500)).getRetransmission();
        assertEquals(Duration.ofMillis(500), noRetransmission.next(1));
        assertEquals(Duration.ZERO, noRetransmission.next(2));

        RetransmissionBackOff twoRetransmissions = Reliability.defaults().withFixedRetransmission(Duration.ofMillis(500), 2).getRetransmission();
        assertEquals(Duration.ofMillis(500), twoRetransmissions.next(1));
        assertEquals(Duration.ofMillis(500), twoRetransmissions.next(3));
        assertEquals(Duration.ZERO, twoRetransmissions.next(4));
    }

    @Test
    void shouldSetExponentialRetransmission() {
        RetransmissionBackOff retransmission = Reliability.defaults().withExponentialRetransmission(Duration.ofMillis(100), 2).getRetransmission();

        assertBetween(100, 150, retransmission.next(1));
        assertBetween(200, 300, retransmission.next(2));
        assertBetween(400, 600, retransmission.next(3));
        assertEquals(Duration.ZERO, retransmission.next(4));
    }

    private static void assertBetween(long minMillis, long maxMillis, Duration actual) {
        assertTrue(actual.toMillis() >= minMillis && actual.toMillis() <= maxMillis, "Expected " + minMillis + ".." + maxMillis + " ms, got " + actual);
    }

    @Test
    void shouldDisableDuplicateDetection() {
        assertFalse(Reliability.defaults().withoutDuplicateDetection().getDuplicateDetection().isEnabled());
    }

    @Test
    void shouldCreateNewSuppliers_whenNotSet() {
        Reliability reliability = Reliability.defaults();

        assertNotSame(reliability.resolveMessageIdSupplier(), reliability.resolveMessageIdSupplier());
        assertNotSame(reliability.resolveRequestTagSupplier(), reliability.resolveRequestTagSupplier());
    }

    @Test
    void shouldFail_whenInvalidValues() {
        Reliability reliability = Reliability.defaults();

        assertThrows(IllegalArgumentException.class, () -> reliability.withResponseTimeout(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> reliability.withResponseTimeout(Duration.ZERO));
        assertThrows(NullPointerException.class, () -> reliability.withRetransmission(null));
        assertThrows(IllegalArgumentException.class, () -> reliability.withFixedRetransmission(Duration.ofMillis(500), -1));
        assertThrows(IllegalArgumentException.class, () -> reliability.withExponentialRetransmission(Duration.ofMillis(500), -1));
        assertThrows(NullPointerException.class, () -> reliability.withMessageIdSupplier(null));
        assertThrows(NullPointerException.class, () -> reliability.withRequestTagSupplier(null));
        assertThrows(NullPointerException.class, () -> reliability.withDuplicateDetection(null));
    }
}
