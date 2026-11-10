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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class MessageTypeTest {

    @Test
    public void shouldReturnCorrectCode() {
        assertEquals(0, MessageType.CON.getCode());
        assertEquals(1, MessageType.NON.getCode());
        assertEquals(2, MessageType.ACK.getCode());
        assertEquals(3, MessageType.RST.getCode());
    }

    @Test
    public void shouldDecodeFromCode() throws CoapException {
        assertEquals(MessageType.CON, MessageType.fromCode(0));
        assertEquals(MessageType.NON, MessageType.fromCode(1));
        assertEquals(MessageType.ACK, MessageType.fromCode(2));
        assertEquals(MessageType.RST, MessageType.fromCode(3));
    }

    @Test
    public void shouldThrowExceptionForInvalidCode() {
        assertThrows(CoapException.class, () -> MessageType.fromCode(-1));
        assertThrows(CoapException.class, () -> MessageType.fromCode(4));
        assertThrows(CoapException.class, () -> MessageType.fromCode(99));
    }

    @Test
    public void shouldHaveExpectedStringRepresentation() {
        assertEquals("CON", MessageType.CON.toString());
        assertEquals("NON", MessageType.NON.toString());
        assertEquals("ACK", MessageType.ACK.toString());
        assertEquals("RST", MessageType.RST.toString());
    }
}
