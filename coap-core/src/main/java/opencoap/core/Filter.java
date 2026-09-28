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
package opencoap.core;

import java.util.concurrent.CompletableFuture;

/*
Filter is a type-preserving transformer of a 'handler' that may intercept and transform inputs and outputs.
 */
@FunctionalInterface
public interface Filter<REQ, RES> extends MappingFilter<REQ, RES, REQ, RES> {

    default Filter<REQ, RES> andThen(Filter<REQ, RES> next) {
        return (request, handler) -> {
            Handler<REQ, RES> nextHandler = request2 -> next.apply(request2, handler);
            return apply(request, nextHandler);
        };
    }

    default Filter<REQ, RES> andThenIf(boolean condition, Filter<REQ, RES> next) {
        if (condition) {
            return andThen(next);
        } else {
            return this;
        }
    }

    @SuppressWarnings("PMD.UseDiamondOperator") // looks like PMD bug
    static <REQ, RES> Filter<REQ, RES> identity() {
        return new Filter<REQ, RES>() {
            @Override
            public CompletableFuture<RES> apply(REQ request, Handler<REQ, RES> handler) {
                return handler.apply(request);
            }

            @Override
            public <REQ2, RES2> MappingFilter<REQ, RES, REQ2, RES2> andThen(MappingFilter<REQ, RES, REQ2, RES2> next) {
                return next;
            }

            @Override
            public Filter<REQ, RES> andThen(Filter<REQ, RES> next) {
                return next;
            }

            @Override
            public Handler<REQ, RES> then(Handler<REQ, RES> handler) {
                return handler;
            }
        };
    }

}
