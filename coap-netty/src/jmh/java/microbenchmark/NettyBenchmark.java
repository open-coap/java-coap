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
package microbenchmark;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.utils.Bytes;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opencoap.coap.netty.NettyCoapTransport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;

@State(Scope.Benchmark)
@Threads(1)
@Fork(value = 1, jvmArgsPrepend = {"-Xms128m", "-Xmx128m"})
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 1, time = 20)
public class NettyBenchmark {

    private CoapServer coapServer;
    private NettyCoapTransport clientTransport = new NettyCoapTransport(createNettyBootstrap(), EMPTY_RESOLVER);
    private NettyCoapTransport serverTransport = new NettyCoapTransport(createNettyBootstrap(), EMPTY_RESOLVER);
    private CoapPacket coapRequest;
    private CoapPacket coapResp;

    @Setup
    public void setup() throws Exception {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.ERROR);

        coapServer = new CoapServer(serverTransport, coapIn -> serverTransport.sendPacket(coapResp), null, () -> {
        });
        coapServer.start();
        clientTransport.start();

        InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", serverTransport.getLocalSocketAddress().getPort());
        InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", clientTransport.getLocalSocketAddress().getPort());

        coapRequest = newCoapPacket(serverAddress).mid(313).get().uriPath("/test").build();
        coapResp = newCoapPacket(clientAddress).mid(313).ack(Code.C205_CONTENT).payload(Bytes.opaqueOfRandom(1024)).build();
    }

    @TearDown
    public void tearDown() {
        clientTransport.stop();
        coapServer.stop();
    }


    @Benchmark
    @OperationsPerInvocation(1)
    public void udpTransport_1_outgoing_transaction(Blackhole bh) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<CoapPacket> cliReceived = clientTransport.receive();
        assertTrue(clientTransport.sendPacket(coapRequest).get(5, TimeUnit.SECONDS));

        assertNotNull(cliReceived.get(5, TimeUnit.SECONDS));
        // assertEquals(Code.C205_CONTENT, cliReceived.join().getCode());

        bh.consume(cliReceived);
    }

    private final int maxTransactions = 80;

    private CompletableFuture<Integer> loopReceive(int attemptsLeft) {
        if (attemptsLeft == 0) {
            return CompletableFuture.completedFuture(attemptsLeft);
        }
        return clientTransport.receive().thenCompose(resp -> loopReceive(attemptsLeft - 1));
    }

    @Benchmark
    @OperationsPerInvocation(maxTransactions)
    public void udpTransport_80_concurrent_transactions(Blackhole bh) throws InterruptedException, ExecutionException, TimeoutException {
        // receive in loop
        CompletableFuture<Integer> rcvPromise = loopReceive(maxTransactions);

        for (int i = 0; i < maxTransactions; i++) {
            clientTransport.sendPacket(coapRequest);
        }

        rcvPromise.get(1, TimeUnit.SECONDS);
    }

    private static Bootstrap createNettyBootstrap() {
        EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("udp", true));
        // EventLoopGroup group = new KQueueEventLoopGroup(1, new DefaultThreadFactory("udp", true));
        // EventLoopGroup group = new EpollEventLoopGroup(1, new DefaultThreadFactory("udp", true));

        return new Bootstrap()
                .group(group)
                .localAddress(0)
                .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))
                .channel(NioDatagramChannel.class)
                //.channel(KQueueDatagramChannel.class)
                // .channel(EpollDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        // do nothing
                    }
                });
    }
    // MacBook Pro 2020 (intel)
    // udpTransport_100_concurrent_transactions:
    //      NIO    direct: 90.5k trx/s
    //      NIO      heap: 88.9k trx/s
    //      KQueue direct: 93.8k trx/s
    //      KQueue   heap: 91.9k trx/s

}
