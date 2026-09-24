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
package opencoap.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class ContentFormatTest {

    @Test
    public void should_return_null_for_unknown_content_format() {
        assertNull(ContentFormat.contentFormatToString(null));
        assertNull(ContentFormat.contentFormatToString((short) -2));
        assertNull(ContentFormat.contentFormatToString((short) 1));
        assertNull(ContentFormat.contentFormatToString((short) 9745));
        assertNull(ContentFormat.contentFormatToString((short) 164));

        assertNull(ContentFormat.parseContentFormat(null));
        assertNull(ContentFormat.parseContentFormat("non/existing"));
    }

    @Test
    public void contentTypeConverterTest() {
        assertEquals("text/plain", ContentFormat.contentFormatToString((short) 0));
        assertEquals("application/xml", ContentFormat.contentFormatToString((short) 41));
        assertEquals("application/octet-stream", ContentFormat.contentFormatToString((short) 42));
        assertEquals("application/exi", ContentFormat.contentFormatToString((short) 47));
        assertEquals("application/json", ContentFormat.contentFormatToString((short) 50));
        assertEquals("application/link-format", ContentFormat.contentFormatToString((short) 40));

        assertEquals("application/cbor", ContentFormat.contentFormatToString((short) 60));
        assertEquals("application/cose; cose-type=\"cose-mac\"", ContentFormat.contentFormatToString((short) 97));
        assertEquals("application/cose; cose-type=\"cose-sign1\"", ContentFormat.contentFormatToString((short) 18));
        assertEquals("application/coap-group+json", ContentFormat.contentFormatToString((short) 256));


        assertEquals((Short) ContentFormat.CT_TEXT_PLAIN, ContentFormat.parseContentFormat("text/plain"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_EXI, ContentFormat.parseContentFormat("application/exi"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_JSON, ContentFormat.parseContentFormat("application/json"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_LINK__FORMAT, ContentFormat.parseContentFormat("application/link-format"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_OCTET__STREAM, ContentFormat.parseContentFormat("application/octet-stream"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_XML, ContentFormat.parseContentFormat("application/xml"));

        assertEquals((Short) ContentFormat.CT_APPLICATION_CODE_ENCRYPT0, ContentFormat.parseContentFormat("application/cose; cose-type=\"cose-encrypt0\""));
        assertEquals((Short) ContentFormat.CT_APPLICATION_CODE_KEY, ContentFormat.parseContentFormat("application/cose-key"));
        assertEquals((Short) ContentFormat.CT_APPLICATION_MERGE_PATCH_JSON, ContentFormat.parseContentFormat("application/merge-patch+json"));
    }
}
