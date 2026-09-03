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
package opencoap.packet;

import static opencoap.packet.DataConvertingUtility.parseUriQuery;
import static opencoap.packet.DataConvertingUtility.parseUriQueryMult;
import static opencoap.packet.DataConvertingUtility.percentEncodeUriQueryOption;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataConvertingUtilityTest {

    @Test
    public void testParseUriQuery() throws ParseException {
        Map<String, String> q = new HashMap<>();
        q.put("par1", "12");

        assertEquals(q, parseUriQuery("par1=12"));
        assertEquals(q, parseUriQuery("?par1=12"));

        q.put("par2", "14");
        assertEquals(q, parseUriQuery("par1=12&par2=14"));
        assertEquals(q, parseUriQuery("?par1=12&par2=14"));

        q.put("d", "b");
        assertEquals(q, parseUriQuery("par1=12&par2=14&d=b"));

        assertNull(parseUriQuery(null));
        assertNull(parseUriQuery(""));
    }

    @Test
    public void testParseUriQueryWithoutValue() throws ParseException {
        Map<String, String> q = new HashMap<>();
        q.put("par1", "");

        assertEquals(q, parseUriQuery("par1"));
        assertEquals(q, parseUriQuery("?par1"));
        assertEquals(q, parseUriQuery("par1="));
    }


    @Nested
    class PercentEncodeUriQueryOptionTest {

        @Test
        public void shouldPercentEncodeCharactersOutsideTheAllowedSet() {
            assertEquals("filter=a%26b", percentEncodeUriQueryOption("filter=a&b"));
            assertEquals("hello%20world", percentEncodeUriQueryOption("hello world"));
            assertEquals("100%25", percentEncodeUriQueryOption("100%"));
            assertEquals("a%22b%3Cc%3Ed%23e", percentEncodeUriQueryOption("a\"b<c>d#e"));
        }

        @Test
        public void shouldEncodeExactlyTheCharactersOutsideTheRfc7252QuerySet() {
            // spelled out from RFC 7252 6.5 step 8: unreserved / sub-delims except '&' / ':' '@' '/' '?'
            String unreserved = "-._~";
            String subDelimsWithoutSeparator = "!$'()*+,;=";
            String alsoAllowed = ":@/?";

            StringBuilder literal = new StringBuilder();
            StringBuilder encoded = new StringBuilder();
            for (char chr = 0x20; chr <= 0x7E; chr++) {
                String single = String.valueOf(chr);
                if (percentEncodeUriQueryOption(single).equals(single)) {
                    literal.append(chr);
                } else {
                    encoded.append(chr);
                    assertEquals(String.format("%%%02X", (int) chr), percentEncodeUriQueryOption(single));
                }
            }

            for (char chr : (unreserved + subDelimsWithoutSeparator + alsoAllowed).toCharArray()) {
                assertTrue(literal.indexOf(String.valueOf(chr)) >= 0, chr + " must stay literal");
            }
            assertEquals(" \"#%&<>[\\]^`{|}", encoded.toString());
        }

        @Test
        public void shouldPercentEncodeUtf8BytesInUppercase() {
            assertEquals("za%C5%BC%C3%B3%C5%82%C4%87", percentEncodeUriQueryOption("zażółć"));
        }

        @Test
        public void shouldNotEncodePlusSign() {
            // '+' is a sub-delim, so RFC 7252 leaves it literal. It does not survive a consumer that
            // decodes as application/x-www-form-urlencoded, where a bare '+' means space -- such a
            // consumer needs its own encoding, this method is not it.
            assertEquals("current_version=1.2.3+build.7", percentEncodeUriQueryOption("current_version=1.2.3+build.7"));
        }

        @Test
        public void shouldEncodeOneOptionSoSeparatorIsNotTreatedAsSuch() {
            // encodes a single option, so '&' is data and gets escaped rather than passed through
            assertEquals("a=1%26b=2", percentEncodeUriQueryOption("a=1&b=2"));
        }

        @Test
        public void shouldEncodeEmptyValueToEmptyString() {
            assertEquals("", percentEncodeUriQueryOption(""));
        }
    }

    @Test
    public void splitTest() throws Exception {
        assertArrayEquals(new String[]{"", "test1", "test2", "test3"},
                DataConvertingUtility.split("/test1/test2/test3", '/'));

        assertArrayEquals(new String[]{"", "test1", "", "test3"},
                DataConvertingUtility.split("/test1//test3", '/'));

        assertArrayEquals(new String[]{"aa"},
                DataConvertingUtility.split("aa", '/'));

    }

    @Test
    public void testParseMultiUriQuery() throws ParseException {
        Map<String, List<String>> q = new HashMap<>();
        q.put("par1", Lists.newArrayList("12"));
        assertEquals(q, parseUriQueryMult("par1=12"));

        Map<String, List<String>> q2 = new HashMap<>();
        q2.put("par1", Lists.newArrayList("11", "22"));
        assertEquals(q2, parseUriQueryMult("par1=11&par1=22"));

        assertNull(parseUriQueryMult(null));
        assertNull(parseUriQueryMult(""));

        assertThatThrownBy(() -> parseUriQueryMult("p=aa&par133")).isExactlyInstanceOf(ParseException.class);
    }

}
