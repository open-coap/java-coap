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
package com.mbed.coap.server;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.utils.Networks.localhost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.transport.InMemoryCoapTransport;
import org.junit.jupiter.api.Test;

class CoapServerGroupTest {
    private int port = 10_000;
    private final CoapServerBuilder builder = CoapServer.builder()
            .transport(() -> InMemoryCoapTransport.create(port++))
            .route(RouterService.builder()
                    .get("/test", (req) -> CoapResponse.ok("test").toFuture())
            );


    @Test
    void test() throws Exception {
        // given
        CoapServerGroup servers = builder.buildGroup(3).start();
        assertEquals(3, servers.getTransports().size());
        CoapClient client = builder.buildClient(localhost(10_001));
        CoapClient client2 = builder.buildClient(localhost(10_003));

        // when
        assertEquals("test", client.sendSync(get("/test")).getPayloadString());
        assertEquals("test", client2.sendSync(get("/test")).getPayloadString());

        // then
        client.close();
        client2.close();
        servers.stop();
        assertFalse(servers.isRunning());
    }

    @Test
    void failWhenBuildingGroupWithNoServers() {
        assertThrows(IllegalArgumentException.class, () -> builder.buildGroup(0));
    }

}
