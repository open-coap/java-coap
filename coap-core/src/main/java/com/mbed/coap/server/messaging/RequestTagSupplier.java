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
package com.mbed.coap.server.messaging;

import com.mbed.coap.packet.Opaque;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@FunctionalInterface
public interface RequestTagSupplier {
    RequestTagSupplier NULL = () -> null;

    Opaque next();

    static RequestTagSupplier createSequential() {
        return createSequential(new Random().nextInt(0xFFFF));
    }

    static RequestTagSupplier createSequential(int init) {
        final AtomicInteger current = new AtomicInteger(init);

        return () -> Opaque.variableUInt(current.incrementAndGet());
    }
}
