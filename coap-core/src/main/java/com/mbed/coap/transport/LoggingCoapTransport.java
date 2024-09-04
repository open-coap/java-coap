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
package com.mbed.coap.transport;

import com.mbed.coap.packet.CoapPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCoapTransport implements CoapTransport {
    private static Logger LOGGER = LoggerFactory.getLogger(LoggingCoapTransport.class);

    private final CoapTransport transport;

    public LoggingCoapTransport(CoapTransport transport) {
        this.transport = transport;
    }

    @Override
    public void start() throws IOException {
        transport.start();
    }

    @Override
    public void stop() {
        transport.stop();
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(CoapPacket coapPacket) {
        return transport.sendPacket(coapPacket).whenComplete((sent, error) -> {
            logSent(coapPacket, error);
        });
    }

    @Override
    public CompletableFuture<CoapPacket> receive() {
        return transport.receive().whenComplete((packet, __) -> {
            if (packet != null) {
                logReceived(packet);
            }
        });
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return transport.getLocalSocketAddress();
    }

    private void logSent(CoapPacket packet, Throwable maybeError) {
        if (maybeError != null) {
            LOGGER.warn("[{}] CoAP sent failed [{}] {}", packet.getRemoteAddrString(), packet.toString(false, false, false, true), maybeError.toString());
            return;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CoAP sent [{}]", packet.toString(true));
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("CoAP sent [{}]", packet.toString(false));
        } else if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] CoAP sent [{}]", packet.getRemoteAddrString(), packet.toString(false, false, false, true));
        }
    }

    private void logReceived(CoapPacket packet) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CoAP received [{}]", packet.toString(true));
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] CoAP received [{}]", packet.getRemoteAddrString(), packet.toString(false));
        } else if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] CoAP received [{}]", packet.getRemoteAddrString(), packet.toString(false, false, false, true));
        }
    }
}
