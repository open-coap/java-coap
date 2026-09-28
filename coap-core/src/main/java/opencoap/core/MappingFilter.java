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
import java.util.function.BiFunction;
import java.util.function.Function;

/*
MappingFilter is a transformer of a 'handler' that may intercept and transform inputs and outputs across types.
 */
@FunctionalInterface
public interface MappingFilter<REQ, RES, IN_REQ, IN_RES> extends BiFunction<REQ, Handler<IN_REQ, IN_RES>, CompletableFuture<RES>> {

    @Override
    CompletableFuture<RES> apply(REQ request, Handler<IN_REQ, IN_RES> handler);

    default <REQ2, RES2> MappingFilter<REQ, RES, REQ2, RES2> andThen(MappingFilter<IN_REQ, IN_RES, REQ2, RES2> next) {
        return (request, handler) -> {
            Handler<IN_REQ, IN_RES> nextHandler = request2 -> next.apply(request2, handler);
            return apply(request, nextHandler);
        };
    }

    default MappingFilter<REQ, RES, IN_REQ, IN_RES> andThenIf(boolean condition, MappingFilter<IN_REQ, IN_RES, IN_REQ, IN_RES> next) {
        if (condition) {
            return andThen(next);
        } else {
            return this;
        }
    }

    default <REQ2> MappingFilter<REQ, RES, REQ2, IN_RES> andThenMap(Function<IN_REQ, REQ2> nextFunc) {
        return this.andThen((request, handler) ->
                handler.apply(nextFunc.apply(request))
        );
    }

    default Handler<REQ, RES> then(Handler<IN_REQ, IN_RES> function) {
        return request -> apply(request, function);
    }

    static <REQ, RES, IN_REQ, IN_RES> MappingFilter<REQ, RES, IN_REQ, IN_RES> of(Function<REQ, IN_REQ> nextFunc, Function<IN_RES, RES> respMapFunc) {
        return (request, handler) -> handler
                .apply(nextFunc.apply(request))
                .thenApply(respMapFunc);
    }

}
