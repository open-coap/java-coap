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
package com.mbed.coap.cli.providers;

import static com.mbed.coap.cli.KeystoreUtils.readCAs;
import com.mbed.coap.cli.TransportProvider;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.transport.CoapTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import kotlin.jvm.functions.Function1;
import org.opencoap.ssl.CertificateAuth;
import org.opencoap.ssl.PskAuth;
import org.opencoap.ssl.SslConfig;
import org.opencoap.ssl.SslSession;
import org.opencoap.ssl.transport.DtlsTransmitter;
import org.opencoap.ssl.transport.Transport;
import org.opencoap.transport.mbedtls.MbedtlsCoapTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MbedtlsProvider implements TransportProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MbedtlsProvider.class);

    private final boolean forceNewHandshake;
    private final int bindPort;

    public MbedtlsProvider(Boolean forceNewHandshake, int bindPort) {
        this.forceNewHandshake = forceNewHandshake;
        this.bindPort = bindPort;
    }

    @Override
    public CoapTransport createUDP(InetSocketAddress destAdr, KeyStore ks, Pair<String, Opaque> psk) throws GeneralSecurityException, IOException {
        SslConfig config;
        if (psk != null) {
            config = SslConfig.client(new PskAuth(psk.key, psk.value.getBytes()));
        } else if (ks != null) {
            config = SslConfig.client(CertificateAuth.trusted(readCAs(ks)));
        } else {
            config = SslConfig.client(CertificateAuth.trusted(), Collections.emptyList(), false);
        }
        File fileSession = new File(destAdr.getHostName() + "-" + destAdr.getPort() + ".session");

        byte[] sessionBytes = readBytes(fileSession);

        DtlsTransmitter transport;
        if (!forceNewHandshake && sessionBytes.length > 0) {
            SslSession session = config.loadSession(new byte[0], sessionBytes, destAdr);
            transport = DtlsTransmitter.create(destAdr, session, bindPort);
        } else {
            transport = DtlsTransmitter.connect(destAdr, config, bindPort).join();
        }

        return MbedtlsCoapTransport.of(new PersistOnStopDtlsTransport(transport, fileSession), transport.getRemoteAddress());
    }

    private static void writeBytes(File fileSession, byte[] bytes) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileSession, false)) {
            fileOutputStream.write(bytes);
        }
    }

    private static byte[] readBytes(File fileSession) throws IOException {
        if (!fileSession.exists() || fileSession.length() <= 0) {
            return new byte[0];
        }

        try (FileInputStream fileInputStream = new FileInputStream(fileSession)) {
            byte[] sessionBytes = new byte[(int) fileSession.length()];
            fileInputStream.read(sessionBytes);
            return sessionBytes;
        }
    }

    private static class PersistOnStopDtlsTransport implements Transport<ByteBuffer> {
        private final DtlsTransmitter underlying;
        private final File fileSession;

        private PersistOnStopDtlsTransport(DtlsTransmitter underlying, File fileSession) {
            this.underlying = underlying;
            this.fileSession = fileSession;
        }

        @Override
        public void close() throws IOException {
            if (underlying.getPeerCid() != null) {
                writeBytes(fileSession, underlying.saveSession());
                LOGGER.info("Stored DTLS session into: {}", fileSession);
            }
        }

        @Override
        public int localPort() {
            return underlying.localPort();
        }

        @Override
        public <P2> Transport<P2> map(Function1<? super ByteBuffer, ? extends P2> function1, Function1<? super P2, ? extends ByteBuffer> function2) {
            return underlying.map(function1, function2);
        }

        @Override
        public CompletableFuture<ByteBuffer> receive(Duration duration) {
            return underlying.receive(duration);
        }

        @Override
        public CompletableFuture<Boolean> send(ByteBuffer byteBuffer) {
            return underlying.send(byteBuffer);
        }
    }
}
