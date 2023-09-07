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
package protocolTests;

import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Opaque.decodeHex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import com.mbed.coap.server.filter.EtagGeneratorFilter;
import com.mbed.coap.transport.InMemoryCoapTransport;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientTest {

    private CoapServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = CoapServer.builder()
                .transport(InMemoryCoapTransport.create(5683))
                .route(RouterService.builder()
                        .get("/test", req -> ok("OK!").toFuture())
                        .post("/fresh", this::handleFresh))
                .build()
                .start();

    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void clientWithExtraOutboundFilters() throws IOException, CoapException {

        CoapClient client = CoapServer.builder()
                .transport(InMemoryCoapTransport.create())
                .outboundFilter(EtagGeneratorFilter.PAYLOAD_HASHING)
                .buildClient(InMemoryCoapTransport.createAddress(5683));

        CoapResponse coapResponse = client.sendSync(CoapRequest.get("/test"));

        assertEquals(CoapResponse.ok("OK!").etag(decodeHex("01a624")), coapResponse);

        client.close();
    }

    @Test
    void shouldRequestFreshResource() throws IOException {
        CoapClient client = CoapServer.builder()
                .transport(InMemoryCoapTransport.create())
                .buildClient(InMemoryCoapTransport.createAddress(5683));

        CompletableFuture<CoapResponse> resp = client.send(CoapRequest.post("/fresh"));

        assertEquals("is fresh", resp.join().getPayloadString());
    }

    private CompletableFuture<CoapResponse> handleFresh(CoapRequest request) {
        if (!Objects.equals(request.options().getEcho(), Opaque.of("13"))) {
            return coapResponse(Code.C401_UNAUTHORIZED)
                    .options(it -> it.echo(Opaque.of("13"))).toFuture();
        }

        return coapResponse(Code.C201_CREATED).payload("is fresh").toFuture();
    }


}
