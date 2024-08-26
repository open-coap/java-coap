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
package org.opencoap.coap.netty;

import static com.mbed.coap.utils.Validations.require;
import static java.util.stream.Collectors.toList;
import static org.opencoap.coap.netty.CoapCodec.EMPTY_RESOLVER;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapServerBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

public class MultiCoapServer {

    private final List<CoapServer> servers;

    private MultiCoapServer(List<CoapServer> servers) {
        require(!servers.isEmpty(), "At least one server required");
        this.servers = servers;
    }

    public static MultiCoapServer create(CoapServerBuilder builder, Bootstrap bootstrap) {
        return create(builder, bootstrap, Integer.MAX_VALUE);
    }

    public static MultiCoapServer create(CoapServerBuilder builder, Bootstrap bootstrap, int limitInstances) {
        EventLoopGroup eventLoopGroup = bootstrap.config().group();

        // create as many servers as there are executors in the event loop group
        List<CoapServer> servers = StreamSupport.stream(eventLoopGroup.spliterator(), false)
                .limit(limitInstances)
                .map(executor -> builder
                        .transport(new NettyCoapTransport(bootstrap, EMPTY_RESOLVER))
                        .executor(executor)
                        .build()
                )
                .collect(toList());

        return new MultiCoapServer(servers);
    }

    public MultiCoapServer start() throws IOException {
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

    public List<Integer> getLocalPorts() {
        return servers.stream().map(server -> server.getLocalSocketAddress().getPort()).collect(toList());
    }
}
