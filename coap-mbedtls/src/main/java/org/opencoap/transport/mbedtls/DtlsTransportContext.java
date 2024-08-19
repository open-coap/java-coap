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

import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.TransportContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import org.opencoap.ssl.netty.DatagramPacketWithContext;
import org.opencoap.ssl.transport.DtlsSessionContext;

public class DtlsTransportContext {
    public static final TransportContext.Key<Map<String, String>> DTLS_AUTHENTICATION = new TransportContext.Key<>(Collections.emptyMap());
    public static final TransportContext.Key<String> DTLS_PEER_CERTIFICATE_SUBJECT = new TransportContext.Key<>(null);
    public static final TransportContext.Key<byte[]> DTLS_CID = new TransportContext.Key<>(null);
    public static final TransportContext.Key<Instant> DTLS_SESSION_START_TIMESTAMP = new TransportContext.Key<>(null);
    public static final TransportContext.Key<Boolean> DTLS_SESSION_EXPIRATION_HINT = new TransportContext.Key<>(false);

    public static final BiFunction<CoapPacket, ChannelHandlerContext, DatagramPacket> DTLS_COAP_TO_DATAGRAM_CONVERTER = (coapPacket, ctx) -> {
        ByteBuf buf = ctx.alloc().buffer(coapPacket.getPayload().size() + 128);
        CoapSerializer.serialize(coapPacket, new ByteBufOutputStream(buf));
        return new DatagramPacketWithContext(buf, coapPacket.getRemoteAddress(), null, DtlsTransportContext.toDtlsSessionContext(coapPacket.getTransportContext()));
    };

    public static TransportContext toTransportContext(DtlsSessionContext dtlsSessionContext) {
        if (dtlsSessionContext.equals(DtlsSessionContext.EMPTY)) {
            return TransportContext.EMPTY;
        }

        TransportContext dtlsContext = TransportContext
                .of(DTLS_AUTHENTICATION, dtlsSessionContext.getAuthenticationContext())
                .with(DTLS_SESSION_EXPIRATION_HINT, dtlsSessionContext.getSessionExpirationHint());
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

    public static DtlsSessionContext toDtlsSessionContext(TransportContext transportContext) {
        return new DtlsSessionContext(
                transportContext.get(DTLS_AUTHENTICATION),
                transportContext.get(DTLS_PEER_CERTIFICATE_SUBJECT),
                transportContext.get(DTLS_CID),
                transportContext.get(DTLS_SESSION_START_TIMESTAMP),
                transportContext.get(DTLS_SESSION_EXPIRATION_HINT)
        );
    }
}
