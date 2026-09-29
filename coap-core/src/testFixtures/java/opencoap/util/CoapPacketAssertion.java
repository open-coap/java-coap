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
package opencoap.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import opencoap.codec.CoapPacket;

public class CoapPacketAssertion {

    public static void assertSimilar(CoapPacket cp1, CoapPacket cp2) {
        assertEquals(cp1.getMethod(), cp2.getMethod());
        assertEquals(cp1.getMessageType(), cp2.getMessageType());
        assertEquals(cp1.getCode(), cp2.getCode());
        assertEquals(cp1.getMessageId(), cp2.getMessageId());

        assertEquals(cp1.options().getBlock1Req(), cp2.options().getBlock1Req());
        assertEquals(cp1.options().getBlock2Res(), cp2.options().getBlock2Res());
        assertEquals(cp1.options().getUriPath(), cp2.options().getUriPath());
        assertEquals(cp1.options().getUriAuthority(), cp2.options().getUriAuthority());
        assertEquals(cp1.options().getUriHost(), cp2.options().getUriHost());
        assertEquals(cp1.options().getUriQueryList(), cp2.options().getUriQueryList());
        assertEquals(cp1.options().getLocationPath(), cp2.options().getLocationPath());
        assertEquals(cp1.options().getLocationQuery(), cp2.options().getLocationQuery());

        assertEquals(cp1.options().getAccept(), cp2.options().getAccept());
        assertArrayEquals(cp1.options().getIfMatch(), cp2.options().getIfMatch());
        assertArrayEquals(cp1.options().getEtagArray(), cp2.options().getEtagArray());

        assertEquals(cp1.options().getIfNoneMatch(), cp2.options().getIfNoneMatch());
        assertEquals(cp1.options().getContentFormat(), cp2.options().getContentFormat());
        assertEquals(cp1.options().getEtag(), cp2.options().getEtag());
        assertEquals(cp1.options().getMaxAge(), cp2.options().getMaxAge());
        assertEquals(cp1.options().getObserve(), cp2.options().getObserve());
        assertEquals(cp1.options().getProxyUri(), cp2.options().getProxyUri());
        assertEquals(cp1.getToken(), cp2.getToken());
        assertEquals(cp1.options().getUriPort(), cp2.options().getUriPort());

        assertEquals(cp1.getPayloadString(), cp2.getPayloadString());

        assertEquals(cp1.getRemoteAddress(), cp2.getRemoteAddress());
        assertEquals(cp1.getAttributes(), cp2.getAttributes());
    }

}
