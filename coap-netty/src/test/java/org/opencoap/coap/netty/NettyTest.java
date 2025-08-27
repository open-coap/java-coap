/*
 * Copyright (C) 2022-2025 java-coap contributors (https://github.com/open-coap/java-coap)
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
package org.opencoap.coap.netty;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static com.mbed.coap.utils.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NettyTest {
    private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("udp", true), NioIoHandler.newFactory());
    private CoapServer server;

    @BeforeAll
    void beforeAll() throws IOException {
        server = CoapServer.builder()
                .transport(new NettyCoapTransport(createBootstrap(0), EMPTY_RESOLVER))
                .executor(eventLoopGroup)
                .route(RouterService.builder()
                        .get("/test", __ -> ok("OK").toFuture())
                        .post("/echo", req -> ok(req.getPayload()).toFuture())
                )
                .build().start();
    }

    @AfterAll
    void afterAll() throws ExecutionException, InterruptedException {
        server.stop();
        eventLoopGroup.shutdown();
    }

    @Test
    void should_exchange_messages() throws Exception {
        // given
        CoapClient client = CoapServer.builder()
                .transport(new NettyCoapTransport(createBootstrap(0), EMPTY_RESOLVER))
                .executor(eventLoopGroup)
                .buildClient(localhost(server.getLocalSocketAddress().getPort()));

        // when
        assertTrue(client.ping().get());
        assertEquals(ok("OK"), client.sendSync(get("/test")));

        client.close();
    }

    @Test
    void should_exchange_messages_with_multiple_clients() throws Exception {
        // given
        InetSocketAddress serverAddress = localhost(server.getLocalSocketAddress().getPort());
        List<CoapClient> clients = new ArrayList(10);
        for (int i = 0; i < 10; i++) {
            clients.add(
                    CoapServer.builder()
                            .transport(new NettyCoapTransport(createBootstrap(0).remoteAddress(serverAddress), EMPTY_RESOLVER))
                            .executor(eventLoopGroup)
                            .buildClient(serverAddress)
            );
        }

        // when
        for (CoapClient client : clients) {
            assertEquals(ok("paska"), client.sendSync(post("/echo").payload("paska")));
        }

        // then
        for (CoapClient client : clients) {
            client.close();
        }
    }

    private Bootstrap createBootstrap(int port) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .localAddress(port)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        // do nothing
                    }
                });
    }

}
