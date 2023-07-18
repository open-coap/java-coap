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
package com.mbed.coap.server;

import static com.mbed.coap.packet.CoapResponse.ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class RouterServiceTest {
    Service<CoapRequest, CoapResponse> simpleHandler =
            (CoapRequest r) -> CompletableFuture.completedFuture(ok(r.getMethod() + " " + r.options().getUriPath()));

    @Test
    public void shouldBuildSimpleService() throws ExecutionException, InterruptedException {
        Service<CoapRequest, CoapResponse> svc = RouterService.builder()
                .get("/test1", simpleHandler)
                .post("/test1", simpleHandler)
                .get("/test2/*", simpleHandler)
                .get("/test3", simpleHandler)
                .build();

        assertEquals("GET /test1", svc.apply(CoapRequest.get("/test1")).get().getPayloadString());
        assertEquals("POST /test1", svc.apply(CoapRequest.post("/test1")).get().getPayloadString());
        assertEquals("GET /test2/prefixed-route", svc.apply(CoapRequest.get("/test2/prefixed-route")).get().getPayloadString());
        assertEquals(Code.C404_NOT_FOUND, svc.apply(CoapRequest.get("/test3/not-prefixed-route")).get().getCode());
    }

    @Test
    public void shouldWrapRoutes() throws ExecutionException, InterruptedException {
        Service<CoapRequest, CoapResponse> svc = RouterService.builder()
                .get("/test1", simpleHandler)
                .post("/test1", simpleHandler)
                .get("/test2/*", simpleHandler)
                .wrapRoutes((CoapRequest req, Service<CoapRequest, CoapResponse> nextSvc) -> CompletableFuture.completedFuture(ok("42")))
                .get("/test3", simpleHandler)
                .build();

        assertEquals("42", svc.apply(CoapRequest.get("/test1")).get().getPayloadString());
        assertEquals("42", svc.apply(CoapRequest.post("/test1")).get().getPayloadString());
        assertEquals("42", svc.apply(CoapRequest.get("/test2/prefixed-route")).get().getPayloadString());
        assertEquals("GET /test3", svc.apply(CoapRequest.get("/test3")).get().getPayloadString());
        assertEquals(Code.C404_NOT_FOUND, svc.apply(CoapRequest.get("/test4")).get().getCode());
    }

    @Test
    public void shouldChangeDefaultHandler() throws ExecutionException, InterruptedException {
        Service<CoapRequest, CoapResponse> svc = RouterService.builder()
                .defaultHandler((CoapRequest r) -> CompletableFuture.completedFuture(ok("OK")))
                .build();

        assertEquals(Code.C205_CONTENT, svc.apply(CoapRequest.get("/test3")).get().getCode());
    }

    @Test
    public void shouldMergeRoutes() throws ExecutionException, InterruptedException {
        RouterService.RouteBuilder builder1 = RouterService.builder()
                .get("/test1", simpleHandler);
        RouterService.RouteBuilder builder2 = RouterService.builder()
                .get("/test2", simpleHandler)
                .mergeRoutes(builder1);

        Service<CoapRequest, CoapResponse> svc = builder2.build();

        assertEquals(Code.C205_CONTENT, svc.apply(CoapRequest.get("/test1")).get().getCode());
        assertEquals(Code.C205_CONTENT, svc.apply(CoapRequest.get("/test2")).get().getCode());
    }

}
