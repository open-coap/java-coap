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

import static java.util.concurrent.CompletableFuture.completedFuture;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.utils.Service;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ObservationHandler implements Service<SeparateResponse, Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationHandler.class.getName());
    private final NotificationsReceiver notificationsReceiver;
    private final ObservationsStore obsRelations;

    ObservationHandler(NotificationsReceiver notificationsReceiver, ObservationsStore obsStore) {
        this.notificationsReceiver = notificationsReceiver;
        this.obsRelations = obsStore;
    }

    private void terminate(SeparateResponse observationResp) {
        obsRelations.remove(observationResp);
    }

    @Override
    public CompletableFuture<Boolean> apply(SeparateResponse observationResp) {
        return completedFuture(notify(observationResp));
    }

    private boolean notify(SeparateResponse observationResp) {
        Integer observe = observationResp.options().getObserve();
        Optional<String> uriPath = obsRelations.resolveUriPath(observationResp);
        if (observe == null && !uriPath.isPresent()) {
            return false;
        }
        if (observe == null || observationResp.getCode() != Code.C205_CONTENT && observationResp.getCode() != Code.C203_VALID) {
            LOGGER.trace("Notification termination [{}]", observationResp);
            terminate(observationResp);
            return true;
        }

        LOGGER.trace("[{}] Notification", observationResp.getPeerAddress());

        if (!uriPath.isPresent()) {
            LOGGER.info("[{}] No observer for token: {}, sending reset", observationResp.getPeerAddress(), observationResp.getToken().toHex());
        }

        return uriPath
                .map(it -> notificationsReceiver.onObservation(it, observationResp))
                .orElse(false);
    }

}
