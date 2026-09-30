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
package opencoap.routing;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Filter;
import opencoap.core.Handler;
import opencoap.core.Method;

public class RoutingHandler implements Handler<CoapRequest, CoapResponse> {

    private final Map<RequestMatcher, Handler<CoapRequest, CoapResponse>> handlers;
    private final List<Entry<RequestMatcher, Handler<CoapRequest, CoapResponse>>> prefixedHandlers;
    public final Handler<CoapRequest, CoapResponse> defaultHandler;

    public static final Handler<CoapRequest, CoapResponse> NOT_FOUND = request -> CoapResponse.notFound().toFuture();

    public static RouteBuilder builder() {
        return new RouteBuilder();
    }

    private RoutingHandler(Map<RequestMatcher, Handler<CoapRequest, CoapResponse>> handlers, Handler<CoapRequest, CoapResponse> defaultHandler) {

        this.handlers = unmodifiableMap(
                handlers.entrySet().stream()
                        .filter(entry -> !entry.getKey().isPrefixed())
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
        );

        this.prefixedHandlers = unmodifiableList(
                handlers.entrySet().stream()
                        .filter(entry -> entry.getKey().isPrefixed())
                        .collect(Collectors.toList())
        );

        this.defaultHandler = defaultHandler;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request) {
        RequestMatcher requestMatcher = new RequestMatcher(request.getMethod(), request.options().getUriPath());

        return handlers
                .getOrDefault(requestMatcher, findHandler(requestMatcher))
                .apply(request);
    }

    private Handler<CoapRequest, CoapResponse> findHandler(RequestMatcher requestMatcher) {
        Handler<CoapRequest, CoapResponse> nextHandler;

        nextHandler = handlers.get(requestMatcher.withAnyMethod());
        if (nextHandler != null) {
            return nextHandler;
        }

        for (Entry<RequestMatcher, Handler<CoapRequest, CoapResponse>> e : prefixedHandlers) {
            if (e.getKey().matches(requestMatcher)) {
                return e.getValue();
            }
        }
        return defaultHandler;
    }

    public static class RouteBuilder {
        private final Map<RequestMatcher, Handler<CoapRequest, CoapResponse>> handlers = new HashMap<>();
        public Handler<CoapRequest, CoapResponse> defaultHandler = NOT_FOUND;
        private Filter<CoapRequest, CoapResponse> filter = Filter.identity();

        public RouteBuilder get(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.GET, uriPath, handler);
        }

        public RouteBuilder post(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.POST, uriPath, handler);
        }

        public RouteBuilder put(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.PUT, uriPath, handler);
        }

        public RouteBuilder delete(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.DELETE, uriPath, handler);
        }

        public RouteBuilder fetch(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.FETCH, uriPath, handler);
        }

        public RouteBuilder patch(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.PATCH, uriPath, handler);
        }

        public RouteBuilder iPatch(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(Method.IPATCH, uriPath, handler);
        }

        public RouteBuilder any(String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            return add(null, uriPath, handler);
        }

        private RouteBuilder add(Method method, String uriPath, Handler<CoapRequest, CoapResponse> handler) {
            handlers.put(new RequestMatcher(method, uriPath), filter.then(handler));
            return this;
        }

        public RouteBuilder defaultHandler(Handler<CoapRequest, CoapResponse> defaultHandler) {
            this.defaultHandler = defaultHandler;
            return this;
        }

        public RouteBuilder mergeRoutes(RouteBuilder otherBuilder) {
            this.handlers.putAll(otherBuilder.handlers);

            return this;
        }

        public RouteBuilder filter(Filter<CoapRequest, CoapResponse> wrapperFilterProducer) {
            this.filter = this.filter.andThen(wrapperFilterProducer);

            return this;
        }

        public Handler<CoapRequest, CoapResponse> build() {
            return new RoutingHandler(handlers, defaultHandler);
        }
    }

    static final class RequestMatcher {
        final Method method;
        final String uriPath;
        private final boolean isPrefixed;

        RequestMatcher(Method method, String uriPath) {
            this.method = method;
            if (uriPath == null) {
                this.isPrefixed = false;
                this.uriPath = "/";
            } else {
                this.isPrefixed = uriPath.endsWith("*");
                if (isPrefixed) {
                    this.uriPath = uriPath.substring(0, uriPath.length() - 1);
                } else {
                    this.uriPath = uriPath;
                }
            }
        }

        public boolean isPrefixed() {
            return isPrefixed;
        }

        public boolean matches(RequestMatcher other) {
            if (method == null) {
                return other.uriPath.startsWith(uriPath);
            }
            return other.method == method && other.uriPath.startsWith(uriPath);
        }

        public RequestMatcher withAnyMethod() {
            return new RequestMatcher(null, uriPath);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RequestMatcher that = (RequestMatcher) o;
            return method == that.method && Objects.equals(uriPath, that.uriPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, uriPath);
        }
    }
}
