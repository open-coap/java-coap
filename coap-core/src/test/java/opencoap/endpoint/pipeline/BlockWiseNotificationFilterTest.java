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
package opencoap.endpoint.pipeline;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.core.BlockSize.S_16;
import static opencoap.core.CoapResponse.ok;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.reset;
import static org.mockito.BDDMockito.verify;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_1_5683;
import java.util.concurrent.CompletableFuture;
import opencoap.core.Opaque;
import opencoap.core.SeparateResponse;
import opencoap.core.Service;
import opencoap.endpoint.Capabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BlockWiseNotificationFilterTest {

    private Capabilities capability = Capabilities.BASE;
    private final BlockWiseNotificationFilter filter = new BlockWiseNotificationFilter(__ -> capability);
    private final Service<SeparateResponse, Boolean> service = Mockito.mock(Service.class);
    private final Opaque token = Opaque.of("1");

    @BeforeEach
    void setUp() {
        reset(service);
        given(service.apply(any())).willReturn(completedFuture(true));

        capability = new Capabilities(16, true);
    }

    @Test
    void shouldPassWhenSmallPayload() {
        // when
        CompletableFuture<Boolean> resp = filter.apply(ok("OK").toSeparate(token, LOCAL_1_5683), service);

        // then
        assertTrue(resp.join());
        verify(service).apply(ok("OK").toSeparate(token, LOCAL_1_5683));
    }

    @Test
    void shouldSendFirstBlockForLargePayload() {
        // when
        CompletableFuture<Boolean> resp = filter.apply(ok("aaaaaaaaaaaaaaabbbbbbbbbccc").toSeparate(token, LOCAL_1_5683), service);

        // then
        assertTrue(resp.join());
        SeparateResponse expected = ok("aaaaaaaaaaaaaaab").size2Res(27).block2Res(0, S_16, true).toSeparate(token, LOCAL_1_5683);
        verify(service).apply(expected);
    }
}