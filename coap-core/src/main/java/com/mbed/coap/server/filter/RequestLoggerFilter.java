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

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

public class RequestLoggerFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private final LoggingEventBuilder loggingEventBuilder;
    private final Formatter msgFormatter;

    RequestLoggerFilter(LoggingEventBuilder loggingEventBuilder, Formatter msgFormatter) {
        this.loggingEventBuilder = loggingEventBuilder;
        this.msgFormatter = msgFormatter;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest req, Service<CoapRequest, CoapResponse> service) {
        long startTime = System.currentTimeMillis();
        return service.apply(req).thenApply((resp) -> {
            long endTime = System.currentTimeMillis();
            loggingEventBuilder.log(() -> msgFormatter.apply(req, resp, endTime - startTime));
            return resp;
        });
    }

    @FunctionalInterface
    public interface Formatter {
        String apply(CoapRequest req, CoapResponse resp, long duration);
    }

    public static class Builder {
        private Logger logger = LoggerFactory.getLogger(RequestLoggerFilter.class);
        private Level logLevel = Level.INFO;
        private Formatter msgFormatter = (req, resp, duration) -> String.format("%s -> %s (%dms)", req, resp, duration);

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder logLevel(Level logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder msgFormatter(Formatter msgFormatter) {
            this.msgFormatter = msgFormatter;
            return this;
        }

        public RequestLoggerFilter build() {
            return new RequestLoggerFilter(logger.atLevel(logLevel), msgFormatter);
        }
    }
}
