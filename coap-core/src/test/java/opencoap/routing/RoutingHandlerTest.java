/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package opencoap.routing;

import static opencoap.core.CoapRequest.get;
import static opencoap.core.CoapResponse.ok;
import static opencoap.util.CoapRequestBuilderFilter.REQUEST_BUILDER_FILTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.ExecutionException;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Handler;
import org.junit.jupiter.api.Test;

class RoutingHandlerTest {
    Handler<CoapRequest, CoapResponse> simpleHandler =
            (CoapRequest r) -> ok(r.getMethod() + " " + r.options().getUriPath()).toFuture();

    @Test
    public void shouldBuildSimpleService() throws ExecutionException, InterruptedException {
        Handler<CoapRequest.Builder, CoapResponse> handler = REQUEST_BUILDER_FILTER.then(RoutingHandler.builder()
                .get("/test1", simpleHandler)
                .post("/test1", simpleHandler)
                .get("/test2/*", simpleHandler)
                .get("/test3", simpleHandler)
                .build());

        assertEquals("GET /test1", handler.apply(CoapRequest.get("/test1")).get().getPayloadString());
        assertEquals("POST /test1", handler.apply(CoapRequest.post("/test1")).get().getPayloadString());
        assertEquals("GET /test2/prefixed-route", handler.apply(get("/test2/prefixed-route")).get().getPayloadString());
        assertEquals(Code.C404_NOT_FOUND, handler.apply(get("/test3/not-prefixed-route")).get().getCode());
    }

    @Test
    public void shouldFilterRoutes() throws ExecutionException, InterruptedException {
        Handler<CoapRequest.Builder, CoapResponse> handler = REQUEST_BUILDER_FILTER.then(RoutingHandler.builder()
                .get("/test3", simpleHandler)
                .filter((CoapRequest req, Handler<CoapRequest, CoapResponse> nextHandler) -> ok("42").toFuture())
                .get("/test1", simpleHandler)
                .post("/test1", simpleHandler)
                .get("/test2/*", simpleHandler)
                .build());

        assertEquals("42", handler.apply(get("/test1")).get().getPayloadString());
        assertEquals("42", handler.apply(CoapRequest.post("/test1")).get().getPayloadString());
        assertEquals("42", handler.apply(get("/test2/prefixed-route")).get().getPayloadString());
        assertEquals("GET /test3", handler.apply(get("/test3")).get().getPayloadString());
        assertEquals(Code.C404_NOT_FOUND, handler.apply(get("/test4")).get().getCode());
    }

    @Test
    public void shouldChangeDefaultHandler() throws ExecutionException, InterruptedException {
        Handler<CoapRequest.Builder, CoapResponse> handler = REQUEST_BUILDER_FILTER.then(RoutingHandler.builder()
                .defaultHandler((CoapRequest r) -> ok("OK").toFuture())
                .build());

        assertEquals(Code.C205_CONTENT, handler.apply(get("/test3")).get().getCode());
    }

    @Test
    public void shouldMergeRoutes() throws ExecutionException, InterruptedException {
        RoutingHandler.RouteBuilder builder1 = RoutingHandler.builder()
                .get("/test1", simpleHandler);
        RoutingHandler.RouteBuilder builder2 = RoutingHandler.builder()
                .get("/test2", simpleHandler)
                .mergeRoutes(builder1);

        Handler<CoapRequest.Builder, CoapResponse> handler = REQUEST_BUILDER_FILTER.then(builder2.build());

        assertEquals(Code.C205_CONTENT, handler.apply(get("/test1")).get().getCode());
        assertEquals(Code.C205_CONTENT, handler.apply(get("/test2")).get().getCode());
    }

}
