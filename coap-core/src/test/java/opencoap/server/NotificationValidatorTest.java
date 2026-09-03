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
package opencoap.server;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static opencoap.packet.CoapResponse.ok;
import static opencoap.packet.Opaque.EMPTY;
import static opencoap.packet.Opaque.ofBytes;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static protocolTests.utils.CoapPacketBuilder.LOCAL_1_5683;
import java.util.concurrent.CompletableFuture;
import opencoap.packet.SeparateResponse;
import opencoap.utils.Service;
import org.junit.jupiter.api.Test;

class NotificationValidatorTest {

    private final NotificationValidator filter = new NotificationValidator();
    private final Service<SeparateResponse, Boolean> service = filter.then(__ -> completedFuture(true));

    @Test
    public void shouldSendNotification() {
        SeparateResponse notif = ok(EMPTY).observe(12).toSeparate(ofBytes(11), LOCAL_1_5683);

        CompletableFuture<Boolean> resp = service.apply(notif);

        assertTrue(resp.join());
    }

    @Test
    public void failToSendNotificationWithout_missingHeaders() {

        //observation is missing
        assertThatThrownBy(() ->
                service.apply(ok(EMPTY).toSeparate(ofBytes(11), LOCAL_1_5683)).join()
        ).isInstanceOf(IllegalArgumentException.class);

        //token is missing
        assertThatThrownBy(() ->
                service.apply(ok(EMPTY).observe(2).toSeparate(EMPTY, LOCAL_1_5683)).join()
        ).isInstanceOf(IllegalArgumentException.class);
    }

}
