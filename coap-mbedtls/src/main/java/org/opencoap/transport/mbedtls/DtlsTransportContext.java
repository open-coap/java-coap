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
package org.opencoap.transport.mbedtls;

import com.mbed.coap.transport.TransportContext;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.opencoap.ssl.transport.DtlsSessionContext;

public class DtlsTransportContext {
    public static final TransportContext.Key<Map<String, String>> DTLS_AUTHENTICATION = new TransportContext.Key<>(Collections.emptyMap());
    public static final TransportContext.Key<String> DTLS_PEER_CERTIFICATE_SUBJECT = new TransportContext.Key<>(null);
    public static final TransportContext.Key<byte[]> DTLS_CID = new TransportContext.Key<>(null);
    public static final TransportContext.Key<Instant> DTLS_SESSION_START_TIMESTAMP = new TransportContext.Key<>(null);

    public static TransportContext toTransportContext(DtlsSessionContext dtlsSessionContext) {
        if (dtlsSessionContext.equals(DtlsSessionContext.EMPTY)) {
            return TransportContext.EMPTY;
        }

        TransportContext dtlsContext = TransportContext.of(DTLS_AUTHENTICATION, dtlsSessionContext.getAuthenticationContext());
        if (dtlsSessionContext.getPeerCertificateSubject() != null) {
            dtlsContext = dtlsContext.with(DTLS_PEER_CERTIFICATE_SUBJECT, dtlsSessionContext.getPeerCertificateSubject());
        }
        if (dtlsSessionContext.getCid() != null) {
            dtlsContext = dtlsContext.with(DTLS_CID, dtlsSessionContext.getCid());
        }
        if (dtlsSessionContext.getSessionStartTimestamp() != null) {
            dtlsContext = dtlsContext.with(DTLS_SESSION_START_TIMESTAMP, dtlsSessionContext.getSessionStartTimestamp());
        }

        return dtlsContext;
    }
}
