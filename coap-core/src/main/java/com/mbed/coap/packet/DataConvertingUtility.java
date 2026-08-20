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
package com.mbed.coap.packet;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that provides static helper methods for creating and parsing CoAP packet
 */
public final class DataConvertingUtility {

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    /** RFC 3986 "sub-delims", without '&amp;' which separates Uri-Query options. */
    private static final String QUERY_SUB_DELIMS = "!$'()*+,;=";

    private DataConvertingUtility() {
        //keep private
    }

    /**
     * Splits string with given character. Unlike String.split(..) this method
     * does not remove empty elements.
     *
     * @param val text to be split
     * @param ch splitting character
     */
    static String[] split(String val, char ch) {
        int offset = 0;
        ArrayList<String> list = new ArrayList<>();
        int nextPos = val.indexOf(ch, offset);

        while (nextPos != -1) {
            list.add(val.substring(offset, nextPos));
            offset = nextPos + 1;
            nextPos = val.indexOf(ch, offset);
        }
        if (offset == 0) {
            return new String[]{val};
        }

        list.add(val.substring(offset));
        return list.toArray(new String[0]);
    }

    public static Map<String, String> parseUriQuery(String uriQuery) {
        if (uriQuery == null || uriQuery.length() == 0) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        String[] params = uriQuery.substring(uriQuery.indexOf('?') + 1).split("&");

        for (String prm : params) {
            String[] p = prm.split("=", 2);
            if (p.length == 2) {
                result.put(p[0], p[1]);
            } else {
                result.put(p[0], "");
            }
        }
        return result;
    }

    /**
     * Percent-encodes the value of a <b>single</b> Uri-Query option for use in a CoAP URI,
     * following the rules of RFC 7252, section 6.5, step 8. Any character outside the "unreserved"
     * set, the "sub-delims" set except '&amp;', and ':', '&#64;', '/' and '?' is replaced by the
     * uppercase percent-encoded form of its UTF-8 bytes.
     *
     * <p>This takes one option, not a whole query string. '&amp;' is a separator that only exists
     * once options are joined into a URI, so it is escaped here: passing <code>"a=1&amp;b=2"</code>
     * yields <code>"a=1%26b=2"</code>, a single option, not two. To encode a complete query use
     * {@link BasicHeaderOptions#getUriQueryEncoded()}.
     *
     * <p>Notably '+', '=', ';' and ',' are <b>not</b> escaped, which is what makes the result a
     * valid CoAP URI query. This is not <code>application/x-www-form-urlencoded</code>: a consumer
     * decoding with those rules, for example an HTTP proxy target, reads a literal '+' as a space.
     *
     * @param value Uri-Query option value, holding already decoded characters
     * @return percent-encoded value
     */
    public static String percentEncodeUriQueryOption(String value) {
        // Encoded by hand because no JDK helper matches these rules. URLEncoder implements
        // application/x-www-form-urlencoded, which escapes '=', emits '+' for space and escapes
        // '+' ',' ';' '/' '?' that must stay literal here. java.net.URI keeps '&' unescaped, since
        // it is legal in a query component, which would make two options indistinguishable from
        // one option whose value contains '&'.
        StringBuilder sb = new StringBuilder(value.length());
        for (byte rawByte : value.getBytes(StandardCharsets.UTF_8)) {
            int chr = rawByte & 0xFF;
            if (isAllowedInUriQuery(chr)) {
                sb.append((char) chr);
            } else {
                sb.append('%').append(HEX_DIGITS[chr >> 4]).append(HEX_DIGITS[chr & 0x0F]);
            }
        }
        return sb.toString();
    }

    private static boolean isAllowedInUriQuery(int chr) {
        return isUnreserved(chr)
                || QUERY_SUB_DELIMS.indexOf(chr) >= 0
                || chr == ':' || chr == '@' || chr == '/' || chr == '?';
    }

    private static boolean isUnreserved(int chr) {
        return chr >= 'a' && chr <= 'z'
                || chr >= 'A' && chr <= 'Z'
                || chr >= '0' && chr <= '9'
                || chr == '-' || chr == '.' || chr == '_' || chr == '~';
    }

    public static Map<String, List<String>> parseUriQueryMult(String uriQuery) throws ParseException {
        if (uriQuery == null || uriQuery.length() == 0) {
            return null;
        }
        Map<String, List<String>> result = new HashMap<>();
        String[] params = uriQuery.substring(uriQuery.indexOf('?') + 1).split("&");

        for (String prm : params) {
            String[] p = prm.split("=", 2);
            if (p.length != 2) {
                throw new ParseException("", 0);
            }
            List<String> values = result.getOrDefault(p[0], new ArrayList<>(1));
            values.add(p[1]);
            result.put(p[0], values);
        }
        return result;
    }

    static Opaque[] extendOption(Opaque[] orig, Opaque extend) {
        if (orig == null || orig.length == 0) {
            return new Opaque[]{extend};
        } else {
            Opaque[] arr = new Opaque[orig.length + 1];
            System.arraycopy(orig, 0, arr, 0, orig.length);
            arr[orig.length] = extend;
            return arr;
        }
    }

    static String extendOption(String orig, Opaque extend, String delimiter, boolean startWithDelimiter) {
        String extOption = orig;
        if (extOption == null) {
            extOption = "";
        }
        if (extOption.length() == 0 && !startWithDelimiter) {
            return extOption + extend.toUtf8String();
        } else {
            return extOption + delimiter + extend.toUtf8String();
        }
    }

}
