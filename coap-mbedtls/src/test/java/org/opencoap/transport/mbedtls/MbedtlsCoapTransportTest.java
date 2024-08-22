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

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Opaque.of;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static com.mbed.coap.utils.Networks.localhost;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_AUTHENTICATION;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import com.mbed.coap.transport.TransportContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencoap.ssl.PskAuth;
import org.opencoap.ssl.SslConfig;
import org.opencoap.ssl.transport.DtlsServerTransport;
import org.opencoap.ssl.transport.DtlsTransmitter;
import org.opencoap.ssl.transport.Packet;
import protocolTests.utils.CoapPacketBuilder;

class MbedtlsCoapTransportTest {

    private final SslConfig clientConf = SslConfig.client(new PskAuth("test", of("secret").getBytes()));
    private final SslConfig serverConf = SslConfig.server(new PskAuth("test", of("secret").getBytes()));

    private DtlsServerTransport dtlsServer;
    private CoapServer coapServer;
    private InetSocketAddress srvAddress;

    @BeforeEach
    void setUp() throws IOException {
        dtlsServer = DtlsServerTransport.create(serverConf);
        coapServer = CoapServer.builder()
                .transport(new MbedtlsCoapTransport(dtlsServer))
                .route(RouterService.builder()
                        .get("/test", it -> ok("OK!").toFuture())
                        .post("/send-malformed", it -> {
                            dtlsServer.send(new Packet<>("acghfh", it.getPeerAddress()).map(MbedtlsCoapTransportTest::toByteBuffer));
                            return completedFuture(CoapResponse.of(Code.C201_CREATED));
                        })
                        .post("/auth", it -> {
                            String name = it.options().getUriQueryMap().get("name");

                            HashMap<String, String> authCtx = new HashMap<>();
                            authCtx.put("auth", name);
                            return CoapResponse.coapResponse(Code.C201_CREATED).addContext(DTLS_AUTHENTICATION, authCtx).toFuture();
                        })
                        .get("/auth", it -> {
                            String name = it.getTransContext(DTLS_AUTHENTICATION).get("auth");
                            if (name != null) {
                                return CoapResponse.ok(name).toFuture();
                            } else {
                                return coapResponse(Code.C401_UNAUTHORIZED).toFuture();
                            }

                        })
                )
                .build();
        coapServer.start();
        srvAddress = new InetSocketAddress("localhost", coapServer.getLocalSocketAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        coapServer.stop();
    }

    @Test
    void shouldConnectUsingMbedtlsTransport() throws IOException, CoapException {
        // given
        MbedtlsCoapTransport clientTrans = MbedtlsCoapTransport.of(DtlsTransmitter.connect(srvAddress, clientConf).join());
        CoapClient coapClient = CoapServer.builder()
                .transport(clientTrans)
                .buildClient(srvAddress);

        // when
        CoapResponse resp = coapClient.sendSync(get("/test"));

        // then
        assertEquals(ok("OK!"), resp);

        assertNotNull(clientTrans.getLocalSocketAddress());
        coapClient.close();
    }

    @Test
    void shouldIgnoreMalformedCoapPacket() throws IOException, CoapException {
        // given
        MbedtlsCoapTransport clientTrans = MbedtlsCoapTransport.of(DtlsTransmitter.connect(srvAddress, clientConf).join());
        CoapClient coapClient = CoapServer.builder()
                .transport(clientTrans)
                .buildClient(srvAddress);

        // when
        assertEquals(CoapResponse.of(Code.C201_CREATED), coapClient.sendSync(post("/send-malformed")));

        // then
        assertEquals(ok("OK!"), coapClient.sendSync(get("/test")));

        coapClient.close();
    }

    @Test
    void shouldUpdateAndPassDtlsContext() throws IOException, CoapException {
        // given
        MbedtlsCoapTransport clientTrans = MbedtlsCoapTransport.of(DtlsTransmitter.connect(srvAddress, clientConf).join());
        CoapClient coapClient = CoapServer.builder()
                .transport(clientTrans)
                .buildClient(srvAddress);
        // and not authenticated
        assertEquals(Code.C401_UNAUTHORIZED, coapClient.sendSync(get("/auth")).getCode());

        // when
        assertEquals(Code.C201_CREATED, coapClient.sendSync(post("/auth").query("name", "dev-007")).getCode());

        // then
        assertEquals(ok("dev-007"), coapClient.sendSync(get("/auth")));

        assertNotNull(clientTrans.getLocalSocketAddress());
        coapClient.close();
    }

    @Test
    void deserialize_coap_from_bytebuffer_packet_with_offset() {
        // given
        CoapPacket coap = CoapPacketBuilder.newCoapPacket(localhost(5684)).mid(13).get().uriPath("/test").context(TransportContext.EMPTY).build();
        // buffer with offset
        ByteBuffer buffer = ByteBuffer.allocate(200);
        buffer.position(10);
        buffer.put(CoapSerializer.serialize(coap));
        buffer.flip();
        buffer.position(10);
        Packet<ByteBuffer> packet = new Packet<>(buffer, localhost(5684));

        // when
        CoapPacket coap2 = MbedtlsCoapTransport.deserializeCoap(packet);

        //then
        assertEquals(coap, coap2);
    }

    private static ByteBuffer toByteBuffer(String s) {
        return ByteBuffer.wrap(s.getBytes());
    }

}
