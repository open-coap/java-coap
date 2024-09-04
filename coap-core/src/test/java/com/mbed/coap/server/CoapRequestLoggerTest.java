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
import static com.mbed.coap.utils.CoapRequestBuilderFilter.REQUEST_BUILDER_FILTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.utils.Filter;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class CoapRequestLoggerTest {
    private Logger logger = (Logger) LoggerFactory.getLogger(CoapRequestLoggerTest.class);
    private CoapRequestLogger.CoapRequestLoggerBuilder loggerFilterBuilder = CoapRequestLogger.builder().logger(logger);
    private ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @BeforeEach
    public void beforeEach() {
        logger.addAppender(listAppender);
        listAppender.list.clear();
        listAppender.start();
    }

    @Test
    void shouldLogRequestAndResponseWithDefaultMessage() {
        // given
        Filter<CoapRequest.Builder, CoapResponse, CoapRequest, CoapResponse> filter = REQUEST_BUILDER_FILTER.andThen(loggerFilterBuilder.build());
        filter.apply(
                get("/"), __ -> CoapResponse.ok().toFuture()
        );

        // then
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertEquals(Level.INFO, logEvent.getLevel());
        assertTrue(logEvent.getMessage().contains("GET"));
        assertTrue(logEvent.getMessage().contains("205"));
    }

    @Test
    void shouldLogRequestAndResponseWithCustomParameters() {
        // given
        Filter<CoapRequest.Builder, CoapResponse, CoapRequest, CoapResponse> filter = REQUEST_BUILDER_FILTER.andThen(
                loggerFilterBuilder.mdc("callerId", __ -> "ASDF")
                        .level(org.slf4j.event.Level.DEBUG)
                        .formatter((req, resp) -> "[" + req.getPeerAddress() + "] " + req.getMethod() + " " + req.options().getUriPath() + " -> " + resp.getCode().codeToString())
                        .build()
        );
        filter.apply(
                get("/dupa").address(new InetSocketAddress("1.1.1.1", 31337)), __ -> CoapResponse.ok().toFuture()
        );

        // then
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertEquals(Level.DEBUG, logEvent.getLevel());
        assertEquals(logEvent.getMDCPropertyMap().get("callerId"), "ASDF");
        assertEquals(logEvent.getMessage(), "[/1.1.1.1:31337] GET /dupa -> 205");
    }

}
