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
package opencoap.server.messaging;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.utils.Bytes.opaqueOfSize;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_5683;
import static protocolTests.utils.CoapPacketBuilder.newCoapPacket;
import java.util.concurrent.CompletableFuture;
import opencoap.exception.CoapException;
import opencoap.packet.CoapPacket;
import opencoap.utils.Service;
import org.junit.jupiter.api.Test;

class PayloadSizeVerifierTest {

    private final CapabilitiesStorageImpl csmStorage = new CapabilitiesStorageImpl();
    private PayloadSizeVerifier<Boolean> verifier = new PayloadSizeVerifier<>(csmStorage);
    private final Service<CoapPacket, Boolean> service = __ -> completedFuture(true);

    @Test
    public void shouldThrowExceptionWhenTooLargePayload() {
        CompletableFuture<Boolean> resp = verifier.apply(newCoapPacket(LOCAL_5683).post().uriPath("/").payload(opaqueOfSize(2000)).build(), service);

        assertTrue(resp.isCompletedExceptionally());
        assertThatThrownBy(resp::get).hasCauseExactlyInstanceOf(CoapException.class);
    }

    @Test
    public void shouldForwardWhenPayloadSizeIsOK() {
        csmStorage.put(LOCAL_5683, new Capabilities(2000, true));

        CompletableFuture<Boolean> resp = verifier.apply(newCoapPacket(LOCAL_5683).post().uriPath("/").payload(opaqueOfSize(2000)).build(), service);

        assertTrue(resp.join());
    }


}