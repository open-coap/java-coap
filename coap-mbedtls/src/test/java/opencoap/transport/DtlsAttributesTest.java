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
package opencoap.transport;

import static opencoap.transport.DtlsAttributes.DTLS_AUTHENTICATION;
import static opencoap.transport.DtlsAttributes.DTLS_CID;
import static opencoap.transport.DtlsAttributes.DTLS_PEER_CERTIFICATE_SUBJECT;
import static opencoap.transport.DtlsAttributes.DTLS_SESSION_START_TIMESTAMP;
import static opencoap.transport.DtlsAttributes.DTLS_SESSION_SUSPENSION_HINT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.Collections;
import opencoap.core.MessageAttributes;
import org.junit.jupiter.api.Test;
import org.opencoap.ssl.transport.DtlsSessionContext;

public class DtlsAttributesTest {

    @Test
    void shouldConvertEmptyDtlsSessionContext() {
        MessageAttributes attrs = DtlsAttributes.toAttributes(DtlsSessionContext.EMPTY);

        assertTrue(attrs.get(DTLS_AUTHENTICATION).isEmpty());
        assertNull(attrs.get(DTLS_PEER_CERTIFICATE_SUBJECT));
        assertNull(attrs.get(DTLS_CID));
        assertNull(attrs.get(DTLS_SESSION_START_TIMESTAMP));
        assertFalse(attrs.get(DTLS_SESSION_SUSPENSION_HINT));

        assertEquals(attrs, MessageAttributes.EMPTY);
    }

    @Test
    void shouldConvertDtlsSessionContext() {
        MessageAttributes attrs = DtlsAttributes.toAttributes(
                new DtlsSessionContext(Collections.singletonMap("a", "b"), "CN:aa", new byte[]{1, 2}, Instant.ofEpochSecond(123456789), true)
        );

        assertEquals("b", attrs.get(DTLS_AUTHENTICATION).get("a"));
        assertNull(attrs.get(DTLS_AUTHENTICATION).get("fdsfs"));
        assertEquals("CN:aa", attrs.get(DTLS_PEER_CERTIFICATE_SUBJECT));
        assertEquals(Instant.ofEpochSecond(123456789), attrs.get(DTLS_SESSION_START_TIMESTAMP));
        assertArrayEquals(new byte[]{1, 2}, attrs.get(DTLS_CID));
        assertTrue(attrs.get(DTLS_SESSION_SUSPENSION_HINT));
    }
}
