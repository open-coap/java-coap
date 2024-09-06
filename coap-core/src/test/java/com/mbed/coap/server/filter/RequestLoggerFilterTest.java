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
package com.mbed.coap.server.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.utils.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.server.filter.RequestLoggerFilter.Builder.DEFAULT_FORMATTER;
import static com.mbed.coap.utils.CoapRequestBuilderFilter.REQUEST_BUILDER_FILTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.event.Level.DEBUG;

class RequestLoggerFilterTest {
    private Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggerFilterTest.class);
    private RequestLoggerFilter.Builder logFilterBuilder;
    private ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @BeforeEach
    public void beforeEach() {
        logFilterBuilder = new RequestLoggerFilter.Builder().logger(logger);
        logger.addAppender(listAppender);
        listAppender.list.clear();
        listAppender.start();
    }

    @Test
    void defaultFormatterShouldWork() {
        String logMsg = DEFAULT_FORMATTER.apply(get("/").address(new InetSocketAddress("1.1.1.1", 31337)).build(), CoapResponse.ok().build(), 123);

        assertEquals("[/1.1.1.1:31337] CoapRequest[GET] -> CoapResponse[205] (123ms)", logMsg);
    }

    @Test
    void shouldLogRequestAndResponseWithDefaultMessage() {
        // given
        Filter<CoapRequest.Builder, CoapResponse, CoapRequest, CoapResponse> filter = REQUEST_BUILDER_FILTER.andThen(logFilterBuilder.build());
        filter.apply(
                get("/"), __ -> CoapResponse.ok().toFuture()
        ).join();

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
                logFilterBuilder
                        .msgFormatter((req, resp, __) -> String.format("%s %s -> %s", req.getPeerAddress(), req.getMethod(), req.options().getUriPath(), resp.getCode().codeToString()))
                        .logLevel(DEBUG)
                        .build()
        );
        filter.apply(get("/dupa"), __ -> CoapResponse.ok().toFuture()).join();

        // then
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertEquals(Level.DEBUG, logEvent.getLevel());
        assertEquals("GET /dupa -> 205", logEvent.getMessage());
    }
}
