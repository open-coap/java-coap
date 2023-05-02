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
package org.opencoap.coap.netty;

import static java.util.Objects.requireNonNull;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapSerializer;
import com.mbed.coap.transport.TransportContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;
import java.util.function.Function;

@Sharable
public class CoapCodec extends MessageToMessageCodec<DatagramPacket, CoapPacket> {

    private final Function<DatagramPacket, TransportContext> contextResolver;

    public static final Function<DatagramPacket, TransportContext> EMPTY_RESOLVER = __ -> TransportContext.EMPTY;

    public CoapCodec(Function<DatagramPacket, TransportContext> contextResolver) {
        this.contextResolver = requireNonNull(contextResolver);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        CoapPacket coap = CoapSerializer.deserialize(msg.sender(), new ByteBufInputStream(msg.content()));
        coap.setTransportContext(contextResolver.apply(msg));

        out.add(coap);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CoapPacket msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer(msg.getPayload().size() + 128);
        CoapSerializer.serialize(msg, new ByteBufOutputStream(buf));

        out.add(new DatagramPacket(buf, msg.getRemoteAddress()));
    }
}
