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

import static java.util.Objects.requireNonNull;
import static org.opencoap.coap.netty.NettyUtils.toCompletableFuture;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.TransportContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class NettyCoapTransport implements CoapTransport {

    private Channel channel;
    private final Bootstrap bootstrap;
    private final Function<DatagramPacket, TransportContext> contextResolver;
    private final BiFunction<CoapPacket, ChannelHandlerContext, DatagramPacket> coapToDatagramConverter;
    private CompletableFuture<CoapPacket> receivePromise = new CompletableFuture<>();

    public NettyCoapTransport(Bootstrap bootstrap, Function<DatagramPacket, TransportContext> contextResolver) {
        this(bootstrap, contextResolver, CoapCodec.DEFAULT_CONVERTER);
    }

    public NettyCoapTransport(Bootstrap bootstrap, Function<DatagramPacket, TransportContext> contextResolver, BiFunction<CoapPacket, ChannelHandlerContext, DatagramPacket> coapToDatagramConverter) {
        this.bootstrap = bootstrap;
        this.contextResolver = requireNonNull(contextResolver);
        this.coapToDatagramConverter = requireNonNull(coapToDatagramConverter);
    }

    @Override
    public void start() {
        init(bootstrap.bind().syncUninterruptibly().channel());
    }

    void init(Channel channel) {
        this.channel = channel;
        this.channel.pipeline()
                .addLast("coap-codec", new CoapCodec(contextResolver, coapToDatagramConverter))
                .addLast("coap-inbound", new CoapInbound());
    }

    @Override
    public void stop() {
        channel.close();
        channel.closeFuture().syncUninterruptibly();
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(CoapPacket packet) {
        ChannelPromise channelPromise = channel.newPromise();
        channel.writeAndFlush(packet, channelPromise);

        return toCompletableFuture(channelPromise).thenApply(__ -> true);
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        receivePromise = new CompletableFuture<>();
        return receivePromise;
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    public Channel getChannel() {
        return channel;
    }

    class CoapInbound extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!receivePromise.complete((CoapPacket) msg)) {
                ctx.fireChannelRead(msg);
            }
        }
    }

}
