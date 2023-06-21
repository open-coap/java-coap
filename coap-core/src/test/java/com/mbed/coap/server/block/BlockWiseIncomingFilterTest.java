/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
 * Copyright (C) 2011-2021 ARM Limited. All rights reserved.
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
package com.mbed.coap.server.block;

import static com.mbed.coap.packet.BlockSize.S_16;
import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.post;
import static com.mbed.coap.packet.CoapRequest.put;
import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.packet.CoapResponse.of;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Code.C204_CHANGED;
import static com.mbed.coap.packet.Code.C205_CONTENT;
import static com.mbed.coap.packet.Code.C231_CONTINUE;
import static com.mbed.coap.packet.Opaque.decodeHex;
import static com.mbed.coap.utils.Assertions.assertEquals;
import static com.mbed.coap.utils.Bytes.opaqueOfSize;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapOptionsBuilder;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.messaging.Capabilities;
import com.mbed.coap.utils.Service;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BlockWiseIncomingFilterTest {
    private Capabilities capability = Capabilities.BASE;
    private final BlockWiseIncomingFilter blockingFilter = new BlockWiseIncomingFilter(__ -> capability, 10000000);
    private CoapRequest lastRequest = null;
    private Service<CoapRequest, CoapResponse> service;

    @Test
    void shouldForwardWhenNonBlockRequestAndResponse() {
        Service<CoapRequest, CoapResponse> service = blockingFilter
                .then(__ -> ok("OK").toFuture());

        CompletableFuture<CoapResponse> resp = service.apply(get("/").from(LOCAL_5683));

        assertEquals(ok("OK"), resp.join());
    }

    @Test
    public void should_send_blocks() {
        service = blockingFilter
                .then(req -> {
                    lastRequest = req;
                    return completedFuture(of(C204_CHANGED, "ok"));
                });

        CompletableFuture<CoapResponse> resp;

        //BLOCK 1
        resp = service.apply(
                put("/").token(1001).block1Req(0, S_16, true).payload(opaqueOfSize(16)).to(LOCAL_5683)
        );
        assertEquals(coapResponse(C231_CONTINUE).block1Req(0, S_16, true), resp.join());

        //BLOCK 2
        resp = service.apply(
                put("/").token(2002).block1Req(1, S_16, true).payload(opaqueOfSize(16)).to(LOCAL_5683)
        );
        assertEquals(coapResponse(C231_CONTINUE).block1Req(1, S_16, true), resp.join());

        //BLOCK 3
        resp = service.apply(
                put("/").token(3003).block1Req(2, S_16, false).payload(opaqueOfSize(1)).to(LOCAL_5683)
        );
        assertEquals(coapResponse(C204_CHANGED).block1Req(2, S_16, false).payload("ok"), resp.join());


        assertEquals(put("/").token(3003).block1Req(2, S_16, false).payload(opaqueOfSize(33)).to(LOCAL_5683), lastRequest);
    }

    @Test
    public void should_send_error_when_wrong_second_payload_and_block_size() {
        service = blockingFilter
                .then(__ -> ok("OK").toFuture());

        //BLOCK 1
        service.apply(put("/").block1Req(0, S_16, true).payload(opaqueOfSize(16)).to(LOCAL_5683));
        //BLOCK 2
        CompletableFuture<CoapResponse> resp = service.apply(put("/").block1Req(1, S_16, true).payload(opaqueOfSize(17)).to(LOCAL_5683));

        // then
        assertThatThrownBy(resp::join).hasCause(new CoapCodeException(Code.C400_BAD_REQUEST, "block size mismatch"));
    }

    @Test
    public void should_send_error_when_wrong_first_payload_and_block_size() {
        service = blockingFilter
                .then(__ -> ok("OK").toFuture());

        //BLOCK 1
        CompletableFuture<CoapResponse> resp = service.apply(put("/").block1Req(0, S_16, true).payload(opaqueOfSize(17)).to(LOCAL_5683));

        // then
        assertThatThrownBy(resp::join).hasCause(new CoapCodeException(Code.C400_BAD_REQUEST, "block size mismatch"));
    }

    @Test
    public void shouldSendBlockingResponse_2k_with_BERT() {
        service = blockingFilter
                .then(__ -> ok(opaqueOfSize(2000)).toFuture());
        capability = new Capabilities(1200, true);
        CompletableFuture<CoapResponse> resp;

        //BLOCK 0
        resp = service.apply(get("/large").from(LOCAL_5683));
        assertEquals(coapResponse(C205_CONTENT).block2Res(0, BlockSize.S_1024_BERT, true).payload(opaqueOfSize(1024)), resp.join());

        //BLOCK 1
        resp = service.apply(get("/large").block2Res(1, BlockSize.S_1024_BERT, false).from(LOCAL_5683));
        assertEquals(coapResponse(C205_CONTENT).block2Res(1, BlockSize.S_1024_BERT, false).payload(opaqueOfSize(976)), resp.join());
    }

    @Test
    public void shouldSendBlockingResponse_2k_no_BERT_needed() {
        service = blockingFilter
                .then(__ -> ok(opaqueOfSize(2000)).toFuture());
        capability = new Capabilities(4000, true);

        //when
        CompletableFuture<CoapResponse> resp = service.apply(get("/large").from(LOCAL_5683));

        //then full payload, no blocks
        assertEquals(coapResponse(C205_CONTENT).payload(opaqueOfSize(2000)), resp.join());
    }

    @Test
    public void shouldSendBlockingResponse_10k_with_BERT() {
        service = blockingFilter
                .then(__ -> ok(opaqueOfSize(10000)).toFuture());
        capability = new Capabilities(6000, true);
        CompletableFuture<CoapResponse> resp;

        //BLOCK 0
        resp = service.apply(get("/xlarge").from(LOCAL_5683));
        assertEquals(coapResponse(C205_CONTENT).block2Res(0, BlockSize.S_1024_BERT, true).payload(opaqueOfSize(4096)), resp.join());

        //BLOCK 1
        resp = service.apply(get("/xlarge").block2Res(4, BlockSize.S_1024_BERT, false).from(LOCAL_5683));
        assertEquals(coapResponse(C205_CONTENT).block2Res(4, BlockSize.S_1024_BERT, true).payload(opaqueOfSize(4096)), resp.join());

        //BLOCK 2
        resp = service.apply(get("/xlarge").block2Res(8, BlockSize.S_1024_BERT, false).from(LOCAL_5683));
        assertEquals(coapResponse(C205_CONTENT).block2Res(8, BlockSize.S_1024_BERT, false).payload(opaqueOfSize(1808)), resp.join());
    }

    @Test
    public void should_handle_rearranged_blocks_with_multiple_transactions() {
        // https://datatracker.ietf.org/doc/html/draft-mattsson-core-coap-attacks-03#section-2.4.1
        service = blockingFilter.then(req -> {
            lastRequest = req;
            return coapResponse(C204_CHANGED).payload("ok").toFuture();
        });

        CompletableFuture<CoapResponse> resp;

        //BLOCK 1
        resp = service.apply(
                post("/").token(1001).block1Req(0, S_16, true).options(this::requestTag01).payload("incarcerate     ").to(LOCAL_5683)
        );
        assertEquals(coapResponse(C231_CONTINUE).block1Req(0, S_16, true), resp.join());

        //BLOCK 2 is lost
        // post("/").token(1002).block1Req(1, S_16, false).payload("valjean").to(LOCAL_5683)

        // new transaction starts
        //BLOCK 1
        resp = service.apply(
                post("/").token(1003).block1Req(0, S_16, true).payload("promote         ").to(LOCAL_5683)
        );
        assertEquals(coapResponse(C231_CONTINUE).block1Req(0, S_16, true), resp.join());

        //BLOCK 2 from previous transaction
        resp = service.apply(
                post("/").token(1002).block1Req(1, S_16, false).options(this::requestTag01).payload("valjean").to(LOCAL_5683)
        );

        assertThatThrownBy(resp::join).hasCause(new CoapCodeException(Code.C413_REQUEST_ENTITY_TOO_LARGE, "Mismatch request-tag"));
    }

    private void requestTag01(CoapOptionsBuilder it) {
        it.requestTag(decodeHex("01"));
    }

}
