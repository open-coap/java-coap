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

import static java.util.concurrent.CompletableFuture.completedFuture;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.CompletableFuture;

public class NettyUtils {

    public static <T> CompletableFuture<T> toCompletableFuture(Promise<T> nettyPromise) {

        if (nettyPromise.isSuccess()) {
            return completedFuture(nettyPromise.getNow());
        }

        CompletableFuture<T> promise = new CompletableFuture<>();
        nettyPromise.addListener(future -> {
            if (future.cause() != null) {
                promise.completeExceptionally(future.cause());
            } else {
                promise.complete(nettyPromise.getNow());
            }
        });

        return promise;
    }
}
