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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertThrows(NullPointerException.class, () -> reliability.withMessageIdSupplier(null));
        assertThrows(NullPointerException.class, () -> reliability.withRequestTagSupplier(null));
        assertThrows(NullPointerException.class, () -> reliability.withDuplicateDetection(null));
    }
}
