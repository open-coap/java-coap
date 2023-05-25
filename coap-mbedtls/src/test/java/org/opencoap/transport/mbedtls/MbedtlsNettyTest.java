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
import static com.mbed.coap.packet.CoapResponse.badRequest;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Opaque.of;
import static com.mbed.coap.utils.Networks.localhost;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_AUTHENTICATION;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.toTransportContext;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opencoap.coap.netty.NettyCoapTransport;
import org.opencoap.ssl.PskAuth;
import org.opencoap.ssl.SslConfig;
import org.opencoap.ssl.netty.DatagramPacketWithContext;
import org.opencoap.ssl.netty.DtlsChannelHandler;
import org.opencoap.ssl.netty.DtlsClientHandshakeChannelHandler;
import org.opencoap.ssl.netty.NettyTransportAdapter;
import org.opencoap.ssl.netty.SessionAuthenticationContext;
import org.opencoap.ssl.transport.SessionWriter;
import org.opencoap.ssl.transport.Transport;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MbedtlsNettyTest {
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("udp", true));

    private final SslConfig clientConf = SslConfig.client(new PskAuth("test", of("secret").getBytes()));
    private final SslConfig serverConf = SslConfig.server(new PskAuth("test", of("secret").getBytes()));
    private NettyCoapTransport serverTransport;
    private CoapServer server;
    private InetSocketAddress srvAddress;

    @BeforeAll
    void beforeAll() throws IOException {
        serverTransport = new NettyCoapTransport(createBootstrap(0), dgram ->
                toTransportContext(DatagramPacketWithContext.contextFrom(dgram))
        );

        server = CoapServer.builder()
                .transport(serverTransport)
                .executor(eventLoopGroup)
                .route(RouterService.builder()
                        .get("/test", __ -> completedFuture(ok("OK!")))
                        .post("/echo", req -> completedFuture(ok(req.getPayload())))
                        .get("/dtls-ctx", req -> {
                            String key = req.options().getUriQueryMap().get("key");
                            String ctxValue = req.getTransContext(DTLS_AUTHENTICATION).get(key);
                            if (ctxValue != null) {
                                return completedFuture(ok(ctxValue));
                            } else {
                                return completedFuture(CoapResponse.of(Code.C400_BAD_REQUEST));
                            }

                        })
                )
                .build().start();
        srvAddress = localhost(server.getLocalSocketAddress().getPort());
    }

    @AfterAll
    void afterAll() {
        server.stop();
        eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    @Test
    void handshake_and_exchange_messages() throws Exception {
        // given
        NettyCoapTransport clientTrans = new NettyCoapTransport(createClientBootstrap(srvAddress), EMPTY_RESOLVER);
        CoapClient coapClient = CoapServer.builder()
                .transport(clientTrans)
                .buildClient(srvAddress);

        // when
        CoapResponse resp = coapClient.sendSync(get("/test"));

        // then
        assertEquals(ok("OK!"), resp);

        coapClient.close();
        assertFalse(clientTrans.getChannel().isOpen());
    }

    @Test
    void handshake_and_exchange_messages_with_NettyTransportAdapter() throws Exception {
        // given
        Transport<ByteBuffer> clientTrans = NettyTransportAdapter.connect(clientConf, srvAddress, eventLoopGroup)
                .map(MbedtlsNettyTest::copyAndRelease, Unpooled::wrappedBuffer);

        CoapClient coapClient = CoapServer.builder()
                .transport(MbedtlsCoapTransport.of(clientTrans, srvAddress))
                .buildClient(srvAddress);

        // when
        CoapResponse resp = coapClient.sendSync(get("/test"));

        // then
        assertEquals(ok("OK!"), resp);

        coapClient.close();
    }

    @Test
    void shouldUpdateAndReturnDtlsContext() throws IOException, CoapException {
        // given
        NettyCoapTransport clientTrans = new NettyCoapTransport(createClientBootstrap(srvAddress), EMPTY_RESOLVER);
        CoapClient coapClient = CoapServer.builder()
                .transport(clientTrans)
                .buildClient(srvAddress);
        InetSocketAddress cliAddress = localhost(clientTrans.getLocalSocketAddress().getPort());

        assertEquals(badRequest(), coapClient.sendSync(get("/dtls-ctx").query("key", "dev-id")));

        // when
        serverTransport.getChannel().writeAndFlush(new SessionAuthenticationContext(cliAddress, "dev-id", "dev01"));

        // then
        assertEquals(ok("dev01"), coapClient.sendSync(get("/dtls-ctx").query("key", "dev-id")));


        coapClient.close();
    }


    private Bootstrap createBootstrap(int port) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .localAddress(port)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addFirst("DTLS", new DtlsChannelHandler(serverConf));
                    }
                });
    }

    private Bootstrap createClientBootstrap(InetSocketAddress destinationAddress) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .localAddress(0)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addFirst("DTLS", new DtlsClientHandshakeChannelHandler(clientConf.newContext(destinationAddress), destinationAddress, SessionWriter.NO_OPS));
                    }
                });
    }

    public static ByteBuffer copyAndRelease(ByteBuf buf) {
        ByteBuffer byteBuffer = Unpooled.copiedBuffer(buf).nioBuffer();
        buf.release();
        return byteBuffer;
    }

}
