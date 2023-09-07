/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.packet;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

public class CoapOptionsBuilder {
    private final HeaderOptions options;

    public static CoapOptionsBuilder options() {
        return new CoapOptionsBuilder(new HeaderOptions());
    }

    public static CoapOptionsBuilder from(HeaderOptions options) {
        return new CoapOptionsBuilder(options.duplicate());
    }

    CoapOptionsBuilder(HeaderOptions options) {
        this.options = options;
    }

    public HeaderOptions build() {
        return options;
    }

    public CoapOptionsBuilder observe(Integer observe) {
        options.setObserve(observe);
        return this;
    }

    public CoapOptionsBuilder unsetObserve() {
        options.setObserve(null);
        return this;
    }

    public CoapOptionsBuilder etag(Opaque etag) {
        options.setEtag(etag);
        return this;
    }

    public CoapOptionsBuilder etag(Opaque... etag) {
        options.setEtag(etag);
        return this;
    }

    public CoapOptionsBuilder contentFormat(Short contentFormat) {
        options.setContentFormat(contentFormat);
        return this;
    }

    public CoapOptionsBuilder maxAge(int maxAge) {
        options.setMaxAge((long) maxAge);
        return this;
    }

    public CoapOptionsBuilder block2Res(BlockOption block) {
        options.setBlock2Res(block);
        return this;
    }

    public CoapOptionsBuilder block2Res(int blockNr, BlockSize blockSize, boolean more) {
        return block2Res(new BlockOption(blockNr, blockSize, more));
    }

    public CoapOptionsBuilder unsetBlock2Res() {
        options.setBlock2Res(null);
        return this;
    }

    public CoapOptionsBuilder unsetBlock1Req() {
        options.setBlock1Req(null);
        return this;
    }

    public CoapOptionsBuilder size2Res(int size) {
        options.setSize2Res(size);
        return this;
    }

    public CoapOptionsBuilder unsetSize2Res() {
        options.setSize2Res(null);
        return this;
    }

    public CoapOptionsBuilder block1Req(BlockOption block) {
        options.setBlock1Req(block);
        return this;
    }

    public CoapOptionsBuilder ifNull(Function<HeaderOptions, Object> predicate, Consumer<CoapOptionsBuilder> command) {
        if (predicate.apply(options) == null) {
            command.accept(this);
        }
        return this;
    }

    public CoapOptionsBuilder block1Req(int blockNr, BlockSize blockSize, boolean more) {
        return block1Req(new BlockOption(blockNr, blockSize, more));
    }

    public CoapOptionsBuilder maxAge(Duration maxAge) {
        options.setMaxAge(maxAge.getSeconds());
        return this;
    }

    public CoapOptionsBuilder locationPath(String locationPath) {
        options.setLocationPath(locationPath);
        return this;
    }

    public CoapOptionsBuilder size1(int size) {
        options.setSize1(size);
        return this;
    }

    public CoapOptionsBuilder uriPath(String uriPath) {
        options.setUriPath(uriPath);
        return this;
    }

    public CoapOptionsBuilder accept(Short accept) {
        if (accept != null) {
            options.setAccept(accept);
        }
        return this;
    }

    public CoapOptionsBuilder custom(int optionNumber, Opaque data) {
        options.put(optionNumber, data);
        return this;
    }

    public CoapOptionsBuilder proxyUri(String proxyUri) {
        options.setProxyUri(proxyUri);
        return this;
    }

    public CoapOptionsBuilder host(String uriHost) {
        options.setUriHost(uriHost);
        return this;
    }

    public CoapOptionsBuilder query(String query) {
        options.setUriQuery(query);
        return this;
    }

    public CoapOptionsBuilder query(String name, String val) {
        if (name.isEmpty() || name.contains("=") || name.contains("&") || name.contains("?")
                || val.isEmpty() || val.contains("=") || val.contains("&") || val.contains("?")) {
            throw new IllegalArgumentException("Non valid characters provided in query");
        }
        final StringBuilder query = new StringBuilder();
        if (options.getUriQuery() != null) {
            query.append(options.getUriQuery());
            query.append('&');
        }
        query.append(name).append('=').append(val);
        return query(query.toString());
    }

    public CoapOptionsBuilder ifMatch(Opaque etag) {
        options.setIfMatch(new Opaque[]{etag});
        return this;
    }

    public CoapOptionsBuilder locationQuery(String query) {
        options.setLocationQuery(query);
        return this;
    }

    public CoapOptionsBuilder ifNonMatch() {
        options.setIfNonMatch(true);
        return this;
    }

    public CoapOptionsBuilder proxyScheme(String scheme) {
        options.setProxyScheme(scheme);
        return this;
    }

    public CoapOptionsBuilder echo(Opaque echo) {
        options.setEcho(echo);
        return this;
    }

    public CoapOptionsBuilder requestTag(Opaque requestTag) {
        options.setRequestTag(requestTag);
        return this;
    }
}
