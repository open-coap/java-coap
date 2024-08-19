/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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
package org.opencoap.transport.mbedtls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_AUTHENTICATION;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_CID;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_PEER_CERTIFICATE_SUBJECT;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_SESSION_EXPIRATION_HINT;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_SESSION_START_TIMESTAMP;
import com.mbed.coap.transport.TransportContext;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.opencoap.ssl.transport.DtlsSessionContext;

public class DtlsTransportContextTest {

    @Test
    void shouldConvertEmptyDtlsSessionContext() {
        TransportContext transCtx = DtlsTransportContext.toTransportContext(DtlsSessionContext.EMPTY);

        assertTrue(transCtx.get(DTLS_AUTHENTICATION).isEmpty());
        assertNull(transCtx.get(DTLS_PEER_CERTIFICATE_SUBJECT));
        assertNull(transCtx.get(DTLS_CID));
        assertNull(transCtx.get(DTLS_SESSION_START_TIMESTAMP));
        assertFalse(transCtx.get(DTLS_SESSION_EXPIRATION_HINT));

        assertEquals(transCtx, TransportContext.EMPTY);
    }

    @Test
    void shouldConvertDtlsSessionContext() {
        TransportContext transCtx = DtlsTransportContext.toTransportContext(
                new DtlsSessionContext(Collections.singletonMap("a", "b"), "CN:aa", new byte[]{1, 2}, Instant.ofEpochSecond(123456789), true)
        );

        assertEquals("b", transCtx.get(DTLS_AUTHENTICATION).get("a"));
        assertNull(transCtx.get(DTLS_AUTHENTICATION).get("fdsfs"));
        assertEquals("CN:aa", transCtx.get(DTLS_PEER_CERTIFICATE_SUBJECT));
        assertEquals(Instant.ofEpochSecond(123456789), transCtx.get(DTLS_SESSION_START_TIMESTAMP));
        assertArrayEquals(new byte[]{1, 2}, transCtx.get(DTLS_CID));
        assertTrue(transCtx.get(DTLS_SESSION_EXPIRATION_HINT));
    }
}
