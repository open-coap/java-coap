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
package opencoap.endpoint;

import static java.util.Objects.requireNonNull;
import static opencoap.util.Validations.require;
import java.time.Duration;

/**
 * Message layer reliability settings of a CoAP over UDP server: retransmission, response timeout, message ids,
 * request tags and duplicate detection.
 * <p>
 * Immutable, every {@code with*} method returns a modified copy.
 */
public final class Reliability {
    private static final Reliability DEFAULTS = new Reliability(
            RetransmissionBackOff.ofDefault(), Duration.ofMinutes(2), null, null, DuplicateDetection.cache(10_000)
    );

    private final RetransmissionBackOff retransmission;
    private final Duration responseTimeout;
    private final MessageIdSupplier messageIdSupplier;
    private final RequestTagSupplier requestTagSupplier;
    private final DuplicateDetection duplicateDetection;

    private Reliability(RetransmissionBackOff retransmission, Duration responseTimeout, MessageIdSupplier messageIdSupplier,
            RequestTagSupplier requestTagSupplier, DuplicateDetection duplicateDetection) {
        this.retransmission = retransmission;
        this.responseTimeout = responseTimeout;
        this.messageIdSupplier = messageIdSupplier;
        this.requestTagSupplier = requestTagSupplier;
        this.duplicateDetection = duplicateDetection;
    }

    public static Reliability defaults() {
        return DEFAULTS;
    }

    /**
     * Sets retransmission back-off for confirmable messages (default {@link RetransmissionBackOff#ofDefault()}).
     *
     * @param retransmission retransmission back-off
     * @return modified copy
     */
    public Reliability withRetransmission(RetransmissionBackOff retransmission) {
        return new Reliability(requireNonNull(retransmission), responseTimeout, messageIdSupplier, requestTagSupplier, duplicateDetection);
    }

    /**
     * Sets default timeout for waiting for a response (default 2 minutes).
     *
     * @param responseTimeout response timeout
     * @return modified copy
     */
    public Reliability withResponseTimeout(Duration responseTimeout) {
        require(responseTimeout.toMillis() > 0, "responseTimeout must be positive");
        return new Reliability(retransmission, responseTimeout, messageIdSupplier, requestTagSupplier, duplicateDetection);
    }

    /**
     * Sets message id supplier (default: sequential with random start, one per built server).
     *
     * @param messageIdSupplier message id supplier
     * @return modified copy
     */
    public Reliability withMessageIdSupplier(MessageIdSupplier messageIdSupplier) {
        return new Reliability(retransmission, responseTimeout, requireNonNull(messageIdSupplier), requestTagSupplier, duplicateDetection);
    }

    /**
     * Sets request tag supplier for block-wise transfers (default: sequential, one per built server).
     *
     * @param requestTagSupplier request tag supplier
     * @return modified copy
     */
    public Reliability withRequestTagSupplier(RequestTagSupplier requestTagSupplier) {
        return new Reliability(retransmission, responseTimeout, messageIdSupplier, requireNonNull(requestTagSupplier), duplicateDetection);
    }

    /**
     * Sets duplicate detection of incoming messages (default {@code DuplicateDetection.cache(10_000)}).
     *
     * @param duplicateDetection duplicate detection
     * @return modified copy
     */
    public Reliability withDuplicateDetection(DuplicateDetection duplicateDetection) {
        return new Reliability(retransmission, responseTimeout, messageIdSupplier, requestTagSupplier, requireNonNull(duplicateDetection));
    }

    RetransmissionBackOff getRetransmission() {
        return retransmission;
    }

    Duration getResponseTimeout() {
        return responseTimeout;
    }

    MessageIdSupplier resolveMessageIdSupplier() {
        return messageIdSupplier != null ? messageIdSupplier : new SequentialMessageIdSupplier();
    }

    RequestTagSupplier resolveRequestTagSupplier() {
        return requestTagSupplier != null ? requestTagSupplier : RequestTagSupplier.sequential();
    }

    DuplicateDetection getDuplicateDetection() {
        return duplicateDetection;
    }
}
