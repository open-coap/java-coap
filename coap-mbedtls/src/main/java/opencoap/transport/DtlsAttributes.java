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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import opencoap.codec.CoapPacket;
import opencoap.codec.CoapSerializer;
import opencoap.core.AttributeKey;
import opencoap.core.MessageAttributes;
import org.opencoap.ssl.netty.DatagramPacketWithContext;
import org.opencoap.ssl.transport.DtlsSessionContext;

public class DtlsAttributes {
    public static final AttributeKey<Map<String, String>> DTLS_AUTHENTICATION = AttributeKey.defaulted("DTLS_AUTHENTICATION", Collections.emptyMap());
    public static final AttributeKey<String> DTLS_PEER_CERTIFICATE_SUBJECT = AttributeKey.optional("DTLS_PEER_CERTIFICATE_SUBJECT");
    public static final AttributeKey<byte[]> DTLS_CID = AttributeKey.optional("DTLS_CID");
    public static final AttributeKey<Instant> DTLS_SESSION_START_TIMESTAMP = AttributeKey.optional("DTLS_SESSION_START_TIMESTAMP");
    public static final AttributeKey<Boolean> DTLS_SESSION_SUSPENSION_HINT = AttributeKey.defaulted("DTLS_SESSION_SUSPENSION_HINT", false);

    public static final BiFunction<CoapPacket, ChannelHandlerContext, DatagramPacket> DTLS_COAP_TO_DATAGRAM_CONVERTER = (coapPacket, ctx) -> {
        ByteBuf buf = ctx.alloc().buffer(coapPacket.getPayload().size() + 128);
        CoapSerializer.serialize(coapPacket, new ByteBufOutputStream(buf));
        return new DatagramPacketWithContext(buf, coapPacket.getRemoteAddress(), null, toDtlsSessionContext(coapPacket.getAttributes()));
    };

    public static MessageAttributes toAttributes(DtlsSessionContext dtlsSessionContext) {
        if (dtlsSessionContext.equals(DtlsSessionContext.EMPTY)) {
            return MessageAttributes.EMPTY;
        }

        MessageAttributes dtlsAttributes = MessageAttributes
                .of(DTLS_AUTHENTICATION, dtlsSessionContext.getAuthenticationContext())
                .with(DTLS_SESSION_SUSPENSION_HINT, dtlsSessionContext.getSessionSuspensionHint());
        if (dtlsSessionContext.getPeerCertificateSubject() != null) {
            dtlsAttributes = dtlsAttributes.with(DTLS_PEER_CERTIFICATE_SUBJECT, dtlsSessionContext.getPeerCertificateSubject());
        }
        if (dtlsSessionContext.getCid() != null) {
            dtlsAttributes = dtlsAttributes.with(DTLS_CID, dtlsSessionContext.getCid());
        }
        if (dtlsSessionContext.getSessionStartTimestamp() != null) {
            dtlsAttributes = dtlsAttributes.with(DTLS_SESSION_START_TIMESTAMP, dtlsSessionContext.getSessionStartTimestamp());
        }

        return dtlsAttributes;
    }

    public static DtlsSessionContext toDtlsSessionContext(MessageAttributes attributes) {
        return new DtlsSessionContext(
                attributes.get(DTLS_AUTHENTICATION),
                attributes.get(DTLS_PEER_CERTIFICATE_SUBJECT),
                attributes.get(DTLS_CID),
                attributes.get(DTLS_SESSION_START_TIMESTAMP),
                attributes.get(DTLS_SESSION_SUSPENSION_HINT)
        );
    }
}
