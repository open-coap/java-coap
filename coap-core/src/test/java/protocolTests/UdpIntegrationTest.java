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
package protocolTests;

import static opencoap.transport.udp.DatagramSocketTransport.udp;
import static opencoap.utils.Networks.localhost;
import java.io.IOException;
import opencoap.client.CoapClient;
import opencoap.packet.BlockSize;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.server.CoapServer;
import opencoap.server.filter.TokenGeneratorFilter;
import opencoap.utils.Filter;
import opencoap.utils.Service;

public class UdpIntegrationTest extends IntegrationTestBase {

    @Override
    protected CoapClient buildClient(int port) throws IOException {
        return CoapServer.builder()
                .transport(udp())
                .notificationsReceiver(receiver)
                .blockSize(BlockSize.S_1024)
                .outboundFilter(TokenGeneratorFilter.sequential(1))
                .buildClient(localhost(port));
    }

    @Override
    protected CoapServer buildServer(int port, Filter.SimpleFilter<CoapRequest, CoapResponse> routeFilter, Service<CoapRequest, CoapResponse> route) {
        return CoapServer.builder()
                .blockSize(BlockSize.S_1024)
                .transport(udp(port))
                .routeFilter(routeFilter)
                .route(route)
                .build();
    }

}
