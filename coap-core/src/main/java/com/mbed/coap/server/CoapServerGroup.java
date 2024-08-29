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

import static com.mbed.coap.utils.Validations.require;
import static java.util.stream.Collectors.toList;
import java.io.IOException;
import java.util.List;

public class CoapServerGroup {
    private final List<CoapServer> servers;

    CoapServerGroup(List<CoapServer> servers) {
        require(!servers.isEmpty(), "At least one server required");
        this.servers = servers;
    }

    public CoapServerGroup start() throws IOException {
        for (CoapServer server : servers) {
            server.start();
        }
        return this;
    }

    public void stop() {
        for (CoapServer server : servers) {
            server.stop();
        }
    }

    public boolean isRunning() {
        return servers.stream().allMatch(CoapServer::isRunning);
    }

    public List<Integer> getLocalPorts() {
        return servers.stream().map(server -> server.getLocalSocketAddress().getPort()).collect(toList());
    }
}
