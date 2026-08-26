/*
 * Copyright (C) 2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class CoapConstantsTest {

    @Test
    public void shouldDeriveTimeConstantsFromTransmissionParameters() {
        // values given in RFC 7252, section 4.8.2, for the default transmission parameters
        assertEquals(Duration.ofSeconds(45), CoapConstants.MAX_TRANSMIT_SPAN);
        assertEquals(Duration.ofSeconds(247), CoapConstants.EXCHANGE_LIFETIME);
    }
}
