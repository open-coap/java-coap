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

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CoapRequestLogger implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private final LoggingEventBuilder loggingEventBuilder;
    private final BiFunction<CoapRequest, CoapResponse, String> formatter;
    private final Map<String, Function<CoapRequest, String>> mdcUpdaters;

    CoapRequestLogger(Logger logger, Level level, BiFunction<CoapRequest, CoapResponse, String> formatter, Map<String, Function<CoapRequest, String>> mdcUpdaters) {
        this.loggingEventBuilder = logger.atLevel(level);
        this.formatter = formatter;
        this.mdcUpdaters = mdcUpdaters;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest req, Service<CoapRequest, CoapResponse> service) {
        mdcUpdaters.forEach((key, updater) -> MDC.put(key, updater.apply(req)));
        return service.apply(req).thenApply((resp) -> {
            loggingEventBuilder.log(() -> formatter.apply(req, resp));
            mdcUpdaters.forEach((key, ___) -> MDC.remove(key));
            return resp;
        });
    }

    public static CoapRequestLoggerBuilder builder() {
        return new CoapRequestLoggerBuilder();
    }

    public static class CoapRequestLoggerBuilder {
        private Logger logger = LoggerFactory.getLogger(CoapRequestLogger.class);
        private Level level = Level.INFO;
        private BiFunction<CoapRequest, CoapResponse, String> formatter = (req, resp) -> req.toString() + " -> " + resp.toString();
        private final Map<String, Function<CoapRequest, String>> mdcUpdaters = new HashMap<>();

        public CoapRequestLoggerBuilder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public CoapRequestLoggerBuilder level(Level level) {
            this.level = level;
            return this;
        }

        public CoapRequestLoggerBuilder formatter(BiFunction<CoapRequest, CoapResponse, String> formatter) {
            this.formatter = formatter;
            return this;
        }

        public CoapRequestLoggerBuilder mdc(String key, Function<CoapRequest, String> updater) {
            mdcUpdaters.put(key, updater);
            return this;
        }

        public CoapRequestLogger build() {
            return new CoapRequestLogger(logger, level, formatter, mdcUpdaters);
        }
    }
}
