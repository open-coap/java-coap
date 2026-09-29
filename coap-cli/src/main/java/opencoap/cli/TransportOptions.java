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
package opencoap.cli;

import static opencoap.cli.KeystoreUtils.loadKeystore;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import opencoap.cli.providers.JdkProvider;
import opencoap.cli.providers.MbedtlsProvider;
import opencoap.cli.providers.OpensslProvider;
import opencoap.cli.providers.Pair;
import opencoap.cli.providers.PlainTextProvider;
import opencoap.cli.providers.StandardIoProvider;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Filter;
import opencoap.core.Handler;
import opencoap.core.Opaque;
import opencoap.endpoint.CoapServer;
import opencoap.endpoint.Messaging;
import opencoap.endpoint.TcpCoapServer;
import opencoap.transport.CoapTcpTransport;
import opencoap.transport.CoapTransport;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

class TransportOptions {
    private Pair<String, Opaque> psk;

    @Option(names = {"-s", "--ssl-provider"}, paramLabel = "<ssl-provider>", description = "jdk (default),\nmbedtls (default for DTLS),\nopenssl (requires installed openssl that supports dtls),\nstdio (standard IO)")
    private TransportProviderTypes transportProviderType;

    @Option(names = {"-k", "--key-store"}, description = "KeyStore file (with empty passphrase)")
    private String keystoreFile;

    @Option(names = {"--cipher"}, paramLabel = "<name>", description = "Cipher suite")
    private String cipherSuite;

    @Option(names = {"-f", "--force-new-handshake"}, description = "Force new handshake, only applicable when using DTLS with CID")
    private boolean forceNewHandshake;

    @Option(names = {"--port"}, paramLabel = "<port number>", defaultValue = "0", description = "UDP source port number, default: 0")
    private int sourcePort;

    @Option(names = {"-q", "--quiet"}, paramLabel = "<name>", description = "Force new handshake, only applicable when using DTLS with CID")
    void setQuite(boolean quiet) {
        if (quiet) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger logger = loggerContext.getLogger("root");
            logger.setLevel(Level.WARN);
        }
    }

    @Option(names = {"--psk"}, paramLabel = "<id:hex-secret>", description = "Pre shared key pair")
    void setPsk(String pskPair) {
        psk = Pair.split(pskPair, ':').mapValue(Opaque::decodeHex);
    }

    public final CoapServer create(URI uri, Messaging messaging, Filter<CoapRequest, CoapResponse> outboundFilter, Handler<CoapRequest, CoapResponse> handler) {
        try {
            CoapTransport transport = createTransport(uri);

            if (transport instanceof CoapTcpTransport) {
                return TcpCoapServer.builder()
                        .transport((CoapTcpTransport) transport)
                        .messaging(messaging)
                        .outboundFilter(outboundFilter)
                        .handler(handler)
                        .build();
            } else {
                return CoapServer.builder()
                        .transport(transport)
                        .messaging(messaging)
                        .outboundFilter(outboundFilter)
                        .handler(handler)
                        .build();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TransportProvider createTransportProvider(TransportProviderTypes defaultTpt) {
        if (transportProviderType == null) {
            transportProviderType = defaultTpt;
        }

        switch (transportProviderType) {
            case jdk:
                return new JdkProvider();
            case openssl:
                return new OpensslProvider(cipherSuite);
            case stdio:
                return new StandardIoProvider();
            case mbedtls:
                return new MbedtlsProvider(forceNewHandshake, sourcePort);
        }
        throw new IllegalArgumentException();
    }

    private CoapTransport createTransport(URI uri) throws GeneralSecurityException, IOException {
        InetSocketAddress destAdr = addressFromUri(uri);
        KeyStore ks = loadKeystore(keystoreFile);

        switch (uri.getScheme()) {
            case "coap":
                return new PlainTextProvider(sourcePort).createUDP(destAdr, ks, psk);

            case "coap+tcp":
                return new PlainTextProvider(sourcePort).createTCP(destAdr, ks);

            case "coaps":
                return createTransportProvider(TransportProviderTypes.mbedtls)
                        .createUDP(destAdr, ks, psk);

            case "coaps+tcp":
                return createTransportProvider(TransportProviderTypes.jdk)
                        .createTCP(destAdr, ks);

            default:
                throw new IllegalArgumentException("Scheme not supported: " + uri.getScheme());
        }
    }

    public static InetSocketAddress addressFromUri(URI uri) {
        int port = (uri.getPort() == -1) ? defaultPort(uri.getScheme()) : uri.getPort();

        return new InetSocketAddress(uri.getHost(), port);
    }

    private static int defaultPort(String scheme) {
        switch (scheme) {
            case "coap":
            case "coap+tcp":
                return 5683;

            case "coaps":
            case "coaps+tcp":
                return 5684;

            default:
                throw new IllegalArgumentException("Scheme not supported: " + scheme);
        }
    }

    enum TransportProviderTypes {
        jdk, openssl, stdio, mbedtls
    }
}
