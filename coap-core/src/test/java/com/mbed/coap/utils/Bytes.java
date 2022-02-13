/*
 * Copyright (C) 2022 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.utils;

import com.mbed.coap.packet.Opaque;
import java.util.Arrays;
import java.util.Random;

public class Bytes {

    public static Opaque opaqueOfSize(int newSize) {
        return new Opaque(new byte[newSize]);
    }

    public static Opaque opaqueOfRandom(int size) {
        byte[] randomData = new byte[size];
        new Random().nextBytes(randomData);
        return new Opaque(randomData);
    }

    public static Opaque opaqueOfSize(int val, int size) {
        byte[] result = new byte[size];
        Arrays.fill(result, (byte) val);
        return Opaque.of(result);
    }
}
