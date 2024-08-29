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
package org.opencoap.coap.netty;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.transport.udp.DatagramSocketTransport.udp;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static com.mbed.coap.utils.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import com.mbed.coap.server.RouterService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiChannelNettyTest {
    private static final int THREADS = 3;
    private final Bootstrap bootstrap = createBootstrap();

    @Test
    void test() throws Exception {
        CoapServerBuilder builder = CoapServer.builder()
                .route(RouterService.builder()
                        .get("/test", __ -> ok("OK").toFuture())
                );
        MultiCoapServer coapServer = MultiCoapServer.create(builder, bootstrap).start();
        assertTrue(coapServer.isRunning());

        // verify that all channels are working
        for (int i = 0; i < THREADS; i++) {
            CoapClient client = CoapServer.builder()
                    .transport(udp())
                    .buildClient(localhost(coapServer.getLocalPorts().get(i)));

            assertTrue(client.ping().get());
            assertEquals(ok("OK"), client.sendSync(get("/test")));
        }

        coapServer.stop();
        assertFalse(coapServer.isRunning());
    }

    private Bootstrap createBootstrap() {
        if (Epoll.isAvailable()) {
            return createEpollBootstrap();
        }

        return createNioBootstrap();
    }

    private Bootstrap createNioBootstrap() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(THREADS, new DefaultThreadFactory("udp", true));
        return new Bootstrap()
                .group(eventLoopGroup)
                .localAddress(0) // this will cause server binding to multiple ports
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        // do nothing
                    }
                });
    }

    private Bootstrap createEpollBootstrap() {
        EventLoopGroup eventLoopGroup = new EpollEventLoopGroup(3, new DefaultThreadFactory("udp", true));

        return new Bootstrap()
                .group(eventLoopGroup)
                .option(UnixChannelOption.SO_REUSEPORT, true)
                .localAddress(65001) // bind multiple times on single port
                .channel(EpollDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        // do nothing
                    }
                });
    }
}
