/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
 * Copyright (C) 2011-2021 ARM Limited. All rights reserved.
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
package com.mbed.coap.client;

import static com.mbed.coap.utils.Validations.require;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * CoAP client implementation.
 */
public class CoapClient implements Closeable {

    private final InetSocketAddress destination;
    protected final Service<CoapRequest, CoapResponse> clientService;
    private final Closeable closeable;
    private final Function<CoapResponse, Boolean> resolvePingResponse;
    static final Function<CoapResponse, Boolean> defaultResolvePingResponse = resp -> resp.getCode() == null;

    public static CoapClient create(InetSocketAddress target, CoapServer server) {
        return create(target, server, defaultResolvePingResponse);
    }

    public static CoapClient create(InetSocketAddress target, CoapServer server, Function<CoapResponse, Boolean> resolvePingResponse) {
        require(server.isRunning());
        return new CoapClient(target, server.clientService(), server::stop, resolvePingResponse);
    }

    CoapClient(InetSocketAddress destination, Service<CoapRequest, CoapResponse> clientService, Closeable closeable, Function<CoapResponse, Boolean> resolvePingResponse) {
        this.destination = destination;
        this.clientService = clientService;
        this.closeable = closeable;
        this.resolvePingResponse = resolvePingResponse;
    }

    public CompletableFuture<CoapResponse> send(CoapRequest request) {
        return send(request.modify());
    }

    public CompletableFuture<CoapResponse> send(CoapRequest.Builder request) {
        return clientService.apply(request.address(destination).build());
    }

    public CoapResponse sendSync(CoapRequest request) throws CoapException {
        return await(send(request));
    }

    public CoapResponse sendSync(CoapRequest.Builder request) throws CoapException {
        return sendSync(request.build());
    }

    private static CoapResponse await(CompletableFuture<CoapResponse> future) throws CoapException {
        try {
            return future.get(5, TimeUnit.MINUTES);
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            throw CoapException.wrap(ex);
        }
    }

    public CompletableFuture<Boolean> ping() throws CoapException {
        return clientService.apply(CoapRequest.ping(destination, TransportContext.EMPTY))
                .thenApply(resolvePingResponse);
    }

    /**
     * Close CoAP client connection.
     */
    @Override
    public void close() throws IOException {
        closeable.close();
    }

}
