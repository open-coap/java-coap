/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
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
package opencoap.server.block;

import static opencoap.packet.BlockSize.S_1024_BERT;
import static opencoap.packet.BlockSize.S_16;
import static opencoap.packet.CoapRequest.get;
import static opencoap.packet.CoapRequest.post;
import static opencoap.packet.CoapRequest.put;
import static opencoap.packet.CoapResponse.coapResponse;
import static opencoap.packet.CoapResponse.ok;
import static opencoap.utils.Assertions.assertEquals;
import static opencoap.utils.Bytes.opaqueOfSize;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import opencoap.exception.CoapBlockException;
import opencoap.exception.CoapBlockTooLargeEntityException;
import opencoap.exception.CoapException;
import opencoap.packet.BlockSize;
import opencoap.packet.CoapRequest;
import opencoap.packet.CoapResponse;
import opencoap.packet.Code;
import opencoap.packet.Opaque;
import opencoap.server.messaging.Capabilities;
import opencoap.utils.Service;
import org.junit.jupiter.api.Test;

class BlockWiseOutgoingFilterTest {

    private Capabilities capability = Capabilities.BASE;
    private CompletableFuture<CoapResponse.Builder> promise;
    private CoapRequest lastReq;
    private BlockWiseOutgoingFilter filter = new BlockWiseOutgoingFilter(__ -> capability, 100_000);
    private Service<CoapRequest, CoapResponse> service = filter.then(this::newPromise);

    private CompletableFuture<CoapResponse> newPromise(CoapRequest req) {
        promise = new CompletableFuture<>();
        lastReq = req;
        return promise.thenApply(CoapResponse.Builder::build);
    }

    @Test
    public void shouldMakeNonBlockingRequest() throws Exception {
        CoapRequest req = get("/test").from(LOCAL_5683);

        CompletableFuture<CoapResponse> resp = service.apply(get("/test").from(LOCAL_5683));

        assertEquals(req, lastReq);
        assertFalse(resp.isDone());
    }

    @Test
    public void shouldReceiveNonBlockingResponse() throws Exception {
        CompletableFuture<CoapResponse> respFut = service.apply(get("/test").from(LOCAL_5683));

        assertFalse(respFut.isDone());

        //verify response
        promise.complete(ok("OK"));

        assertEquals(ok("OK"), respFut.get());
    }


    @Test
    public void shouldMakeBlockingRequest_maxMsgSz20() throws Exception {
        CoapRequest req = post("/test").payload("LARGE___PAYLOAD_LARGE___PAYLOAD_").to(LOCAL_5683);
        capability = new Capabilities(20, true);

        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(
                post("/test").size1(32).block1Req(0, S_16, true).payload("LARGE___PAYLOAD_").to(LOCAL_5683)
        );

        //response
        promise.complete(coapResponse(Code.C231_CONTINUE).block1Req(0, S_16, false));


        //BLOCK 1
        assertMakeRequest(
                post("/test").block1Req(1, S_16, false).payload("LARGE___PAYLOAD_").to(LOCAL_5683)
        );
        promise.complete(coapResponse(Code.C204_CHANGED).block1Req(1, S_16, false));

        //verify
        assertTrue(respFut.isDone());
        assertEquals(Code.C204_CHANGED, respFut.get().getCode());
    }


    @Test
    public void shoudFail_toReceive_responseWithIncorrectLastBlockSize() {
        capability = new Capabilities(20, true);

        CoapRequest req = get("/test").from(LOCAL_5683);
        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(get("/test").from(LOCAL_5683));

        //response
        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(0, S_16, true).payload(Opaque.of("0123456789ABCDEF")));

        //BLOCK 1
        assertMakeRequest(get("/test").block2Res(1, S_16, false).from(LOCAL_5683));

        promise.complete(ok("0123456789abcdef_").block2Res(1, S_16, false));

        //verify
        assertTrue(respFut.isDone());
        assertThatThrownBy(() -> respFut.get())
                .hasCauseExactlyInstanceOf(CoapBlockException.class)
                .hasMessageStartingWith("opencoap.exception.CoapBlockException: Last block size mismatch with block option");
    }

    @Test
    public void shouldFail_toReceive_tooLarge_blockingResponse() throws Exception {
        service = new BlockWiseOutgoingFilter(__ -> capability, 2000).then(this::newPromise);
        CoapRequest req = get("/test").from(LOCAL_5683);
        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequestAndReceive(
                get("/test").from(LOCAL_5683),
                coapResponse(Code.C205_CONTENT).block2Res(0, BlockSize.S_1024, true).payload(opaqueOfSize(1024))
        );

        //BLOCK 1
        assertMakeRequestAndReceive(
                get("/test").block2Res(1, BlockSize.S_1024, false).from(LOCAL_5683),
                ok(opaqueOfSize(1000)).block2Res(1, BlockSize.S_1024, false)
        );


        assertTrue(respFut.isCompletedExceptionally());
        assertThatThrownBy(respFut::get).hasCauseExactlyInstanceOf(CoapBlockTooLargeEntityException.class);
    }

    @Test
    public void shouldReceiveBlockingResponse() throws Exception {
        CoapRequest req = get("/test").from(LOCAL_5683);
        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(get("/test").from(LOCAL_5683));

        //response
        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(0, S_16, true).payload("LARGE___PAYLOAD_"));

        //BLOCK 1
        assertMakeRequest(get("/test").block2Res(1, S_16, false).from(LOCAL_5683));

        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(1, S_16, false).payload("LARGE___PAYLOAD_"));

        //verify
        assertTrue(respFut.isDone());
        assertEquals(Code.C205_CONTENT, respFut.get().getCode());
        assertEquals("LARGE___PAYLOAD_LARGE___PAYLOAD_", respFut.get().getPayloadString());
    }

    @Test
    public void shouldReceiveBlockingResponse_with_BERT() throws Exception {
        //based on https://tools.ietf.org/html/draft-ietf-core-coap-tcp-tls-09#section-6.1
        CoapRequest req = get("/status").from(LOCAL_5683);

        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(get("/status").from(LOCAL_5683));

        //response
        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(0, S_1024_BERT, true).payload(opaqueOfSize(3072)));

        //BLOCK 1
        assertMakeRequest(get("/status").block2Res(3, S_1024_BERT, false).from(LOCAL_5683));

        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(3, S_1024_BERT, true).payload(opaqueOfSize(5120)));

        //BLOCK 2
        assertMakeRequest(get("/status").block2Res(8, S_1024_BERT, false).from(LOCAL_5683));

        promise.complete(coapResponse(Code.C205_CONTENT).block2Res(8, S_1024_BERT, false).payload(opaqueOfSize(4711)));


        //verify
        assertTrue(respFut.isDone());
        assertEquals(Code.C205_CONTENT, respFut.get().getCode());
        assertEquals(3072 + 5120 + 4711, respFut.get().getPayload().size());
    }

    @Test
    public void shouldSendBlockingRequest_with_BERT() throws Exception {
        //based on https://tools.ietf.org/html/draft-ietf-core-coap-tcp-tls-09#section-6.2
        capability = new Capabilities(10000, true);

        CoapRequest req = put("/options").payload(opaqueOfSize(8192 + 8192 + 5683)).to(LOCAL_5683);

        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(put("/options").block1Req(0, S_1024_BERT, true).size1(22067).payload(opaqueOfSize(8192)).to(LOCAL_5683));

        promise.complete(coapResponse(Code.C231_CONTINUE).block1Req(0, S_1024_BERT, true));

        //BLOCK 1
        assertMakeRequest(put("/options").block1Req(8, S_1024_BERT, true).payload(opaqueOfSize(8192)).to(LOCAL_5683));

        promise.complete(coapResponse(Code.C231_CONTINUE).block1Req(8, S_1024_BERT, true));

        //BLOCK 2
        assertMakeRequest(put("/options").block1Req(16, S_1024_BERT, false).payload(opaqueOfSize(5683)).to(LOCAL_5683));

        promise.complete(coapResponse(Code.C204_CHANGED).block1Req(16, S_1024_BERT, false));

        //verify
        assertTrue(respFut.isDone());
        assertEquals(Code.C204_CHANGED, respFut.get().getCode());
    }


    @Test
    public void shouldFailSendBlockingRequest_when_blockTransferIsDisabled() throws Exception {
        //based on https://tools.ietf.org/html/draft-ietf-core-coap-tcp-tls-09#section-6.2

        CoapRequest req = put("/options").payload(opaqueOfSize(8192 + 8192 + 5683)).to(LOCAL_5683);

        CompletableFuture<CoapResponse> respFut = service.apply(req);

        assertTrue(respFut.isCompletedExceptionally());
        assertThatThrownBy(() -> respFut.get())
                .hasCause(new CoapException("Block transfers are not enabled for localhost/127.0.0.1:5683 and payload size 22067 > max payload size 1152"));

        assertNull(lastReq);
    }


    @Test
    public void should_continue_block_transfer_after_block_size_change() throws ExecutionException, InterruptedException {
        CoapRequest req = post("/test").payload("LARGE___PAYLOAD_LARGE___PAYLOAD_LARGE___PAYLOAD").to(LOCAL_5683);
        capability = new Capabilities(40, true);

        CompletableFuture<CoapResponse> respFut = service.apply(req);

        //BLOCK 0
        assertMakeRequest(
                post("/test").size1(47).block1Req(0, BlockSize.S_32, true).payload("LARGE___PAYLOAD_LARGE___PAYLOAD_").to(LOCAL_5683)
        );

        //response new size=16
        promise.complete(coapResponse(Code.C231_CONTINUE).block1Req(0, S_16, false));


        //BLOCK 1
        assertMakeRequest(
                post("/test").block1Req(1, S_16, true).payload("LARGE___PAYLOAD_").to(LOCAL_5683)
        );
        promise.complete(coapResponse(Code.C231_CONTINUE).block1Req(1, S_16, false));

        //BLOCK 2
        assertMakeRequest(
                post("/test").block1Req(2, S_16, false).payload("LARGE___PAYLOAD").to(LOCAL_5683)
        );
        promise.complete(coapResponse(Code.C204_CHANGED).block1Req(2, S_16, false));

        //verify
        assertTrue(respFut.isDone());
        assertEquals(Code.C204_CHANGED, respFut.get().getCode());
    }

    private void assertMakeRequestAndReceive(CoapRequest req, CoapResponse.Builder resp) {
        assertMakeRequest(req);

        //response
        assertTrue(promise.complete(resp));
    }

    private void assertMakeRequest(CoapRequest req) {
        assertEquals(req, lastReq);
        lastReq = null;
    }

}
