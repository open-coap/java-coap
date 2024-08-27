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

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapResponse.badRequest;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Code.C201_CREATED;
import static com.mbed.coap.packet.Opaque.of;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static com.mbed.coap.utils.Networks.localhost;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_AUTHENTICATION;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.DTLS_COAP_TO_DATAGRAM_CONVERTER;
import static org.opencoap.transport.mbedtls.DtlsTransportContext.toTransportContext;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.server.RouterService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opencoap.coap.netty.NettyCoapTransport;
import org.opencoap.ssl.PskAuth;
import org.opencoap.ssl.SslConfig;
import org.opencoap.ssl.netty.DatagramPacketWithContext;
import org.opencoap.ssl.netty.DtlsChannelHandler;
import org.opencoap.ssl.netty.DtlsClientHandshakeChannelHandler;
import org.opencoap.ssl.netty.NettyTransportAdapter;
import org.opencoap.ssl.transport.DtlsSessionContext;
import org.opencoap.ssl.transport.SessionWriter;
import org.opencoap.ssl.transport.Transport;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnOs(OS.LINUX)
public class MultithreadedMbedtlsNettyTest {
    private final int threads = 4;
    private final int serverPort = new Random().nextInt(32000) + 32000;
    private final EventLoopGroup eventLoopGroup = new EpollEventLoopGroup(threads, new DefaultThreadFactory("pool", true));

    private final SslConfig clientConf = SslConfig.client(new PskAuth("test", of("secret").getBytes()));
    private final SslConfig serverConf = SslConfig.server(new PskAuth("test", of("secret").getBytes()));

    private final Bootstrap clientBootstrap = new Bootstrap()
            .group(eventLoopGroup)
            .localAddress(0)
            .channel(EpollDatagramChannel.class)
            .handler(new ChannelInitializer<DatagramChannel>() {
                @Override
                protected void initChannel(DatagramChannel ch) {
                    InetSocketAddress destinationAddress = localhost(serverPort);
                    ch.pipeline().addFirst("DTLS", new DtlsClientHandshakeChannelHandler(clientConf.newContext(destinationAddress), destinationAddress, SessionWriter.NO_OPS));
        }
    });

    private final Bootstrap serverBootstrap = new Bootstrap()
            .group(eventLoopGroup)
            .localAddress(serverPort)
            .channel(EpollDatagramChannel.class)
            .option(EpollChannelOption.SO_REUSEPORT, true)
            .handler(new ChannelInitializer<DatagramChannel>() {
                @Override
                protected void initChannel(DatagramChannel ch) {
                    ch.pipeline().addFirst("DTLS", new DtlsChannelHandler(serverConf));
                }
            });

    @AfterAll
    void afterAll() {
        eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    @Test
    void multi_thread_server() throws Exception {
        CoapServerBuilder  serverBuilder = CoapServer.builder()
                .executor(eventLoopGroup)
                .route(RouterService.builder()
                        .post("/echo", req -> CompletableFuture.supplyAsync(() -> ok(req.getPayload()).build()))
                );
        List<CoapServer> servers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            servers.add(
                    serverBuilder
                            .transport(new NettyCoapTransport(serverBootstrap, EMPTY_RESOLVER))
                            .build()
                            .start()
            );
        }

        List<CoapClient> clients = new ArrayList<>();
        InetSocketAddress srvAddr = localhost(serverPort);
        CoapServerBuilder clientBuilder = CoapServer.builder().executor(eventLoopGroup);
        for (int i = 0; i < 100; i++) {
            clients.add(
                    clientBuilder
                            .transport(new NettyCoapTransport(clientBootstrap, EMPTY_RESOLVER))
                            .buildClient(srvAddr)
            );
        }

        for (CoapClient client : clients) {
            assertEquals(ok("paska"), client.sendSync(post("/echo").payload("paska")));
            client.close();
        }

        for (CoapServer srv : servers) {
            srv.stop();
        }
    }
}
