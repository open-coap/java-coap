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
        assertNull(ContentFormat.contentFormatToString((int) -2));
        assertNull(ContentFormat.contentFormatToString((int) 1));
        assertNull(ContentFormat.contentFormatToString((int) 9745));
        assertNull(ContentFormat.contentFormatToString((int) 164));

        assertNull(ContentFormat.parseContentFormat(null));
        assertNull(ContentFormat.parseContentFormat("non/existing"));
    }

    @Test
    public void contentTypeConverterTest() {
        assertEquals("text/plain", ContentFormat.contentFormatToString((int) 0));
        assertEquals("application/xml", ContentFormat.contentFormatToString((int) 41));
        assertEquals("application/octet-stream", ContentFormat.contentFormatToString((int) 42));
        assertEquals("application/exi", ContentFormat.contentFormatToString((int) 47));
        assertEquals("application/json", ContentFormat.contentFormatToString((int) 50));
        assertEquals("application/link-format", ContentFormat.contentFormatToString((int) 40));

        assertEquals("application/cbor", ContentFormat.contentFormatToString((int) 60));
        assertEquals("application/cose; cose-type=\"cose-mac\"", ContentFormat.contentFormatToString((int) 97));
        assertEquals("application/cose; cose-type=\"cose-sign1\"", ContentFormat.contentFormatToString((int) 18));
        assertEquals("application/coap-group+json", ContentFormat.contentFormatToString((int) 256));


        assertEquals((Integer) ContentFormat.TEXT_PLAIN, ContentFormat.parseContentFormat("text/plain"));
        assertEquals((Integer) ContentFormat.APPLICATION_EXI, ContentFormat.parseContentFormat("application/exi"));
        assertEquals((Integer) ContentFormat.APPLICATION_JSON, ContentFormat.parseContentFormat("application/json"));
        assertEquals((Integer) ContentFormat.APPLICATION_LINK_FORMAT, ContentFormat.parseContentFormat("application/link-format"));
        assertEquals((Integer) ContentFormat.APPLICATION_OCTET_STREAM, ContentFormat.parseContentFormat("application/octet-stream"));
        assertEquals((Integer) ContentFormat.APPLICATION_XML, ContentFormat.parseContentFormat("application/xml"));

        assertEquals((Integer) ContentFormat.APPLICATION_COSE_ENCRYPT0, ContentFormat.parseContentFormat("application/cose; cose-type=\"cose-encrypt0\""));
        assertEquals((Integer) ContentFormat.APPLICATION_COSE_KEY, ContentFormat.parseContentFormat("application/cose-key"));
        assertEquals((Integer) ContentFormat.APPLICATION_MERGE_PATCH_JSON, ContentFormat.parseContentFormat("application/merge-patch+json"));
    }
}
