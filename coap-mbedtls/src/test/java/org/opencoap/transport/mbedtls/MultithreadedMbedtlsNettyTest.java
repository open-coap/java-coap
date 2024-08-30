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
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Opaque.of;
import static com.mbed.coap.utils.Networks.localhost;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.awaitility.Awaitility.await;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerGroup;
import com.mbed.coap.server.RouterService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opencoap.coap.netty.NettyCoapTransport;
import org.opencoap.ssl.PskAuth;
import org.opencoap.ssl.SslConfig;
import org.opencoap.ssl.netty.DtlsChannelHandler;
import org.opencoap.ssl.netty.DtlsClientHandshakeChannelHandler;
import org.opencoap.ssl.transport.SessionWriter;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnOs(OS.LINUX)
public class MultithreadedMbedtlsNettyTest {
    private final int threads = 4;
    private final EventLoopGroup eventLoopGroup = new EpollEventLoopGroup(threads, new DefaultThreadFactory("pool", true));

    private final SslConfig clientConf = SslConfig.client(new PskAuth("test", of("secret").getBytes()));
    private final SslConfig serverConf = SslConfig.server(new PskAuth("test", of("secret").getBytes()));

    private final Bootstrap serverBootstrap = new Bootstrap()
            .group(eventLoopGroup)
            .localAddress(0)
            .channel(EpollDatagramChannel.class)
            .option(UnixChannelOption.SO_REUSEPORT, true)
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
        Set<String> usedThreads = new HashSet<>();
        CoapServerGroup server = CoapServer.builder()
                .transport(() -> new NettyCoapTransport(serverBootstrap, EMPTY_RESOLVER))
                .route(RouterService.builder()
                        .get("/currentThread", req -> supplyAsync(() -> ok(currentThread().getName()).build(), eventLoopGroup))
                )
                .buildGroup(threads)
                .start();


        await().pollInterval(Duration.ZERO).atMost(ofSeconds(30)).until(() -> {
            String threadName = connectAndReadCurrentThreadName();
            usedThreads.add(threadName);

            // wait until all threads from eventLoopGroup are used
            return usedThreads.size() == threads;
        });

        server.stop();
    }

    private String connectAndReadCurrentThreadName() throws IOException, CoapException {
        InetSocketAddress serverAddress = localhost(((InetSocketAddress) serverBootstrap.config().localAddress()).getPort());
        Bootstrap clientBootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .remoteAddress(serverAddress)
                .channel(EpollDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addFirst("DTLS", new DtlsClientHandshakeChannelHandler(clientConf.newContext(serverAddress), serverAddress, SessionWriter.NO_OPS));
                    }
                });

        CoapClient client = CoapServer.builder()
                .executor(eventLoopGroup)
                .transport(new NettyCoapTransport(clientBootstrap, EMPTY_RESOLVER))
                .buildClient(serverAddress);

        String threadName = client.sendSync(get("/currentThread")).getPayloadString();
        client.close();
        return threadName;
    }
}
