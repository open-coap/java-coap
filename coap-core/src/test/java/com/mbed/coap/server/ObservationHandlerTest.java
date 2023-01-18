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
package com.mbed.coap.server;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapResponse.notFound;
import static com.mbed.coap.packet.CoapResponse.ok;
import static com.mbed.coap.packet.Opaque.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.reset;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.utils.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocolTests.utils.StubNotificationsReceiver;

class ObservationHandlerTest {

    private final StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
    private final ObservationsStore obsMap = ObservationsStore.inMemory();
    private final ObservationHandler obs = new ObservationHandler(notifReceiver, obsMap);
    private final Service<CoapRequest, CoapResponse> service = mock(Service.class);


    @BeforeEach
    void setUp() {
        reset(service);
        notifReceiver.clear();

        obsMap.add(get("/obs").token(of("100")));
    }

    @Test
    void missingObservationRelation() {
        assertFalse(notif(ok("OK").observe(2).toSeparate(of("999"), null)));
    }

    @Test
    void shouldTerminate() throws InterruptedException {
        // when
        assertTrue(notif(notFound().toSeparate(of("100"), null)));

        // then
        verifyNoObservationRelation(of("100"));
    }

    @Test
    void shouldTerminateWhenMissingObsOption() throws InterruptedException {
        // when
        assertTrue(notif(ok("123").toSeparate(of("100"), null)));

        // then
        verifyNoObservationRelation(of("100"));
    }

    @Test
    void shouldNotify() throws InterruptedException {
        // when
        assertTrue(notif(ok("21C").observe(3).toSeparate(of("100"), null)));

        // then
        notifReceiver.verifyReceived(ok("21C").observe(3));
    }

    private Boolean notif(SeparateResponse observationResp) {
        return obs.apply(observationResp).join();
    }

    private void verifyNoObservationRelation(Opaque token) {
        assertFalse(obsMap.resolveUriPath(ok("21C").observe(3).toSeparate(token, null)).isPresent());
        assertFalse(notif(ok("21C").observe(3).toSeparate(token, null)));
    }

}
