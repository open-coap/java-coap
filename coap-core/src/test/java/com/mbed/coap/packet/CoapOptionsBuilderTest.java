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
package com.mbed.coap.packet;

import static com.mbed.coap.packet.CoapOptionsBuilder.options;
import static com.mbed.coap.packet.Opaque.decodeHex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class CoapOptionsBuilderTest {

    @Test
    public void malformedUriQuery() {
        failQueryWithNonValidChars("", "2");
        failQueryWithNonValidChars("&", "2");
        failQueryWithNonValidChars("=", "54");
        failQueryWithNonValidChars("f", "");
        failQueryWithNonValidChars("f", "&");
        failQueryWithNonValidChars("f", "=");
    }

    private static void failQueryWithNonValidChars(String name, String val) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> options().query(name, val));

        assertEquals("Non valid characters provided in query", e.getMessage());
    }

    @Test
    public void shouldBuildEmpty() {
        HeaderOptions options = options().build();

        assertEquals(new HeaderOptions(), options);
    }

    @Test
    public void shouldBuildWithAllOptions() {
        HeaderOptions options = options()
                .contentFormat(MediaTypes.CT_APPLICATION_JSON)
                .maxAge(Duration.ofHours(1))
                .uriPath("/test")
                .host("host.com")
                .query("param", "value")
                .observe(13)
                .block1Req(1, BlockSize.S_256, false)
                .size1(312)
                .block2Res(2, BlockSize.S_64, true)
                .size2Res(1001)
                .ifMatch(decodeHex("ff"))
                .ifNonMatch()
                .etag(decodeHex("00"))
                .accept(MediaTypes.CT_APPLICATION_CBOR)
                .proxyUri("/proxy")
                .proxyScheme("http")
                .locationPath("/location")
                .locationQuery("par2=val2")
                .echo(decodeHex("248618b4"))
                .requestTag(decodeHex("da611128"))
                .custom(1000, decodeHex("010203"))
                .build();

        HeaderOptions expected = new HeaderOptions();
        expected.setContentFormat(MediaTypes.CT_APPLICATION_JSON);
        expected.setMaxAge(3600L);
        expected.setUriPath("/test");
        expected.setUriHost("host.com");
        expected.setUriQuery("param=value");
        expected.setObserve(13);
        expected.setBlock1Req(new BlockOption(1, BlockSize.S_256, false));
        expected.setSize1(312);
        expected.setBlock2Res(new BlockOption(2, BlockSize.S_64, true));
        expected.setSize2Res(1001);
        expected.setIfMatch(new Opaque[]{decodeHex("ff")});
        expected.setIfNonMatch(true);
        expected.setEtag(decodeHex("00"));
        expected.setAccept(MediaTypes.CT_APPLICATION_CBOR);
        expected.setProxyUri("/proxy");
        expected.setProxyScheme("http");
        expected.setLocationPath("/location");
        expected.setLocationQuery("par2=val2");
        expected.setEcho(decodeHex("248618b4"));
        expected.setRequestTag(decodeHex("da611128"));
        expected.put(1000, decodeHex("010203"));

        assertEquals(expected, options);
    }

    @Test
    public void shouldUnsetOptions() {
        HeaderOptions options = options()
                .observe(41)
                .block2Res(2, BlockSize.S_256, false)
                .block1Req(0, BlockSize.S_256, true)
                .size2Res(535)
                .build();

        HeaderOptions unsetOptions = CoapOptionsBuilder.from(options)
                .unsetObserve()
                .unsetBlock1Req()
                .unsetBlock2Res()
                .unsetSize2Res()
                .build();

        assertEquals(new HeaderOptions(), unsetOptions);
    }

    @Test
    void shouldRunWithIfCondition() {
        CoapOptionsBuilder builder = options()
                .ifNull(HeaderOptions::getAccept, o -> o.accept((short) 321));
        assertEquals(321, builder.build().getAccept());

        // when
        builder.ifNull(HeaderOptions::getAccept, o -> o.accept((short) 43432));

        // then, value not changed
        assertEquals(321, builder.build().getAccept());
    }
}
