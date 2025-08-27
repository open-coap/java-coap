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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NettyUtilsTest {

    private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());

    @AfterEach
    void tearDown() {
        eventLoopGroup.shutdownNow();
    }

    @Test
    void should_convert_netty_promise_to_completable_future() throws ExecutionException, InterruptedException {
        Promise<String> nettyPromise = new DefaultPromise(eventLoopGroup.next());
        CompletableFuture<String> completableFuture = NettyUtils.toCompletableFuture(nettyPromise);
        assertFalse(completableFuture.isDone());

        // when
        nettyPromise.setSuccess("test");

        // then
        assertEquals("test", completableFuture.get());
        assertEquals("test", NettyUtils.toCompletableFuture(nettyPromise).get());

    }

    @Test
    void should_convert_netty_promise_to_completable_future_with_exception() throws ExecutionException, InterruptedException {
        Promise<String> nettyPromise = new DefaultPromise(eventLoopGroup.next());
        CompletableFuture<String> completableFuture = NettyUtils.toCompletableFuture(nettyPromise);
        assertFalse(completableFuture.isDone());

        // when
        nettyPromise.setFailure(new IOException("bad"));

        // then
        assertThrows(ExecutionException.class, completableFuture::get);
        assertTrue(completableFuture.isCompletedExceptionally());


    }
}
