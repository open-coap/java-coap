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
package com.mbed.coap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * CoAP constants that are defined in RFC 7252 document
 */
public final class CoapConstants {

    public static final int DEFAULT_PORT = 5683;
    public static final String WELL_KNOWN_CORE = "/.well-known/core";
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final Duration ACK_TIMEOUT = Duration.ofSeconds(2);
    public static final float ACK_RANDOM_FACTOR = 1.5f;
    public static final Short MAX_RETRANSMIT = 4;

    /**
     * Maximum time from the first transmission of a Confirmable message to its last retransmission, 45 seconds with
     * the default transmission parameters (RFC 7252, section 4.8.2).
     */
    public static final Duration MAX_TRANSMIT_SPAN = Duration.ofMillis(
            (long) (ACK_TIMEOUT.toMillis() * ((1 << MAX_RETRANSMIT) - 1) * ACK_RANDOM_FACTOR)
    );

    /**
     * Maximum time a datagram is expected to take from the start of its transmission to the completion of its
     * reception (RFC 7252, section 4.8.2).
     */
    public static final Duration MAX_LATENCY = Duration.ofSeconds(100);

    /**
     * Time a node takes to turn a Confirmable message around into an acknowledgement (RFC 7252, section 4.8.2).
     */
    public static final Duration PROCESSING_DELAY = ACK_TIMEOUT;

    /**
     * Time from starting to send a Confirmable message until it is no longer expected that an acknowledgement or a
     * reply based on it may arrive, 247 seconds with the default transmission parameters (RFC 7252, section 4.8.2).
     */
    public static final Duration EXCHANGE_LIFETIME = MAX_TRANSMIT_SPAN
            .plus(MAX_LATENCY.multipliedBy(2))
            .plus(PROCESSING_DELAY);

    private CoapConstants() {
    }

}
