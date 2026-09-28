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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class FilterTest {

    private final Handler<String, String> srv = request -> completedFuture("S:" + request);
    private final Filter<String, String> filter = (request, handler) -> handler
            .apply(request)
            .thenApply(resp -> "F(" + resp + ")");

    private final Filter<Integer, String> sumFilter = (request, handler) -> handler.apply(request + 1);
    private final Filter<Integer, String> multiplyFilter = (request, handler) -> handler.apply(request * 2);

    private final Handler<Integer, String> numToStringSrv = request -> completedFuture(request.toString());

    @Test
    public void testHandler() throws ExecutionException, InterruptedException {

        String resp = srv.apply("aa").get();

        assertEquals("S:aa", resp);
    }

    @Test
    public void testFilter() throws ExecutionException, InterruptedException {
        Function<String, CompletableFuture<String>> handlerWithFilter = filter.then(srv);

        String resp = handlerWithFilter.apply("aa").get();

        assertEquals("F(S:aa)", resp);
    }

    @Test
    public void testMultiFilter() throws ExecutionException, InterruptedException {
        Function<String, CompletableFuture<String>> handlerWithFilter = filter
                .andThen(filter)
                .then(srv);

        String resp = handlerWithFilter.apply("aa").get();

        assertEquals("F(F(S:aa))", resp);
    }

    @Test
    public void testMultiFilterWithTypes() throws ExecutionException, InterruptedException {
        Function<Integer, CompletableFuture<String>> handlerWithFilter = multiplyFilter
                .andThen(sumFilter)
                .then(numToStringSrv);

        String resp = handlerWithFilter.apply(100).get();

        assertEquals("201", resp);
    }

    @Test
    public void name() throws ExecutionException, InterruptedException {
        MappingFilter<String, String, String, Integer> f = (request, handler) -> handler
                .apply(request)
                .thenCompose(integer -> completedFuture(Integer.toString(integer + 1)));

        Function<String, CompletableFuture<String>> srv = f.then(s -> completedFuture(Integer.parseInt(s)));

        assertEquals("3", srv.apply("2").get());
    }

    @Test
    void identityFilter() {
        Filter<String, String> identity = Filter.identity();

        assertEquals(srv, identity.then(srv));
        assertEquals(filter, identity.andThen(filter));
    }

    @Test
    void andThenApply() {
        Handler<String, Integer> handler = Filter.<String, Integer>identity()
                .andThenMap(Integer::parseInt)
                .then(CompletableFuture::completedFuture);

        assertEquals(123, handler.apply("123").join());
    }

    @Test
    void andThenIf() {
        Filter<Integer, Integer> multipleFilter = (request, handler) -> handler.apply(request * 2);

        Handler<Integer, Integer> handler = Filter.<Integer, Integer>identity()
                .andThenIf(false, multipleFilter)
                .andThenIf(true, multipleFilter)
                .then(num -> completedFuture(num + 10));

        assertEquals(14, handler.apply(2).join());
        assertEquals(0, handler.apply(-5).join());
    }

    @Test
    void FilterOf() {
        MappingFilter<Integer, Integer, Integer, Integer> filter = MappingFilter.of(it -> it + 1, it -> it - 2);

        assertEquals(0, filter.apply(1, CompletableFuture::completedFuture).join());
        assertEquals(16, filter.apply(17, CompletableFuture::completedFuture).join());
    }
}
