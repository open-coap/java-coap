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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.toTransportContext;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.CoapTransport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.opencoap.ssl.transport.DtlsTransmitter;
import org.opencoap.ssl.transport.Packet;
import org.opencoap.ssl.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MbedtlsCoapTransport implements CoapTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MbedtlsCoapTransport.class);
    private final Transport<Packet<ByteBuffer>> dtlsTransport;

    public MbedtlsCoapTransport(Transport<Packet<ByteBuffer>> dtlsTransport) {
        this.dtlsTransport = dtlsTransport;
    }

    public static MbedtlsCoapTransport of(DtlsTransmitter dtlsTransmitter) {
        InetSocketAddress adr = dtlsTransmitter.getRemoteAddress();

        return new MbedtlsCoapTransport(dtlsTransmitter.map(
                bytes -> new Packet<>(bytes, adr),
                Packet<ByteBuffer>::getBuffer
        ));
    }

    public static MbedtlsCoapTransport of(Transport<ByteBuffer> transport, InetSocketAddress adr) {
        return new MbedtlsCoapTransport(transport.map(
                bytes -> new Packet<>(bytes, adr),
                Packet<ByteBuffer>::getBuffer
        ));
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void stop() {
        try {
            dtlsTransport.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(CoapPacket coapPacket) {
        ByteBuffer buf = ByteBuffer.wrap(CoapSerializer.serialize(coapPacket));
        return dtlsTransport.send(new Packet<>(buf, coapPacket.getRemoteAddress(), DtlsTransportContext.toDtlsSessionContext(coapPacket.getTransportContext())));
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        return dtlsTransport.receive(Duration.ofSeconds(1)).thenCompose(this::deserialize);
    }

    private CompletableFuture<CoapPacket> deserialize(Packet<ByteBuffer> packet) {
        CoapPacket coapPacket = deserializeCoap(packet);
        if (coapPacket != null) {
            return completedFuture(coapPacket);
        }

        // keep waiting
        return receive();
    }

    static CoapPacket deserializeCoap(Packet<ByteBuffer> packet) {
        if (packet.getBuffer().remaining() > 0) {
            try {
                ByteArrayInputStream bufInput = toInputStream(packet.getBuffer());
                CoapPacket coapPacket = CoapSerializer.deserialize(packet.getPeerAddress(), bufInput);
                coapPacket.setTransportContext(toTransportContext(packet.getSessionContext()));
                return coapPacket;
            } catch (CoapException e) {
                LOGGER.warn("[{}] Received malformed coap. {}", packet.getPeerAddress(), e.toString());
            }
        }
        return null;
    }

    private static ByteArrayInputStream toInputStream(ByteBuffer byteBuffer) {
        return new ByteArrayInputStream(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return new InetSocketAddress("0.0." + "0.0", dtlsTransport.localPort());
    }
}
