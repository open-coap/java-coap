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
package com.mbed.coap.server.filter;

import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Code.C401_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EchoFilterTest {

    private final EchoFilter filter = new EchoFilter();

    @Test
    void shouldRetryWithEcho() throws ExecutionException, InterruptedException {
        CompletableFuture<CoapResponse> resp = filter.apply(post("/door/unlock"), this::handle1);

        assertEquals(ok("OK"), resp.get());
    }

    @Test
    void shouldForwardErrorMessage() throws ExecutionException, InterruptedException {
        CompletableFuture<CoapResponse> resp = filter.apply(post("/door/unlock"), __ -> coapResponse(C401_UNAUTHORIZED).toFuture());

        assertEquals(C401_UNAUTHORIZED, resp.get().getCode());
    }

    @Test
    @DisplayName("should not repeat on unauthorized response")
    void test3() throws ExecutionException, InterruptedException {
        CompletableFuture<CoapResponse> resp = filter.apply(post("/door/unlock"), __ ->
                coapResponse(C401_UNAUTHORIZED).options(it -> it.echo(Opaque.of("1"))).toFuture()
        );

        assertEquals(C401_UNAUTHORIZED, resp.get().getCode());
    }

    private CompletableFuture<CoapResponse> handle1(CoapRequest request) {
        if (Objects.equals(Opaque.of("echo1"), request.options().getEcho())) {
            return ok("OK").toFuture();
        } else {
            return CoapResponse.of(C401_UNAUTHORIZED)
                    .options(it -> it.echo(Opaque.of("echo1")))
                    .toFuture();
        }
    }

}
