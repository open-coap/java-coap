/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.server.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class RequestTagSupplierTest {

    @Test
    void shouldGeneratedSequentialRequestTags() {
        RequestTagSupplier requestTagSupplier = RequestTagSupplier.createSequential(0);

        assertEquals("01", requestTagSupplier.next().toHex());
        assertEquals("02", requestTagSupplier.next().toHex());
        assertEquals("03", requestTagSupplier.next().toHex());
    }

    @Test
    void shouldGeneratedSequentialRequestTags_lardInit() {
        RequestTagSupplier requestTagSupplier = RequestTagSupplier.createSequential(0x7ffffffd);

        assertEquals("7ffffffe", requestTagSupplier.next().toHex());
        assertEquals("7fffffff", requestTagSupplier.next().toHex());
        assertEquals("00", requestTagSupplier.next().toHex());
        assertEquals("01", requestTagSupplier.next().toHex());
    }
}
