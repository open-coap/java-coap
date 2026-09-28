/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
 * Copyright (C) 2011-2018 ARM Limited. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Constants from the IANA CoAP Content-Formats registry.
 *
 * <p>Content-Format identifiers are uint16 values (0..65535) as defined by RFC 7252.
 *
 * @see <a href="https://www.iana.org/assignments/core-parameters/core-parameters.xhtml#content-formats">IANA CoAP Content-Formats</a>
 */
public class ContentFormat {

    //RFC 7252
    //RFC 7252
    public static final int TEXT_PLAIN = 0;
    public static final int APPLICATION_LINK_FORMAT = 40; //RFC 6690
    public static final int APPLICATION_XML = 41;
    public static final int APPLICATION_OCTET_STREAM = 42;
    public static final int APPLICATION_EXI = 47;
    public static final int APPLICATION_JSON = 50;
    //--- OMA LwM2M ---
    public static final int APPLICATION_LWM2M_TLV = 11542;
    public static final int APPLICATION_LWM2M_JSON = 11543;
    //RFC8152
    public static final int APPLICATION_COSE_ENCRYPT0 = 16;
    public static final int APPLICATION_COSE_MAC0 = 17;
    public static final int APPLICATION_COSE_SIGN1 = 18;
    public static final int APPLICATION_COSE_ENCRYPT = 96;
    public static final int APPLICATION_COSE_MAC = 97;
    public static final int APPLICATION_COSE_SIGN = 98;
    public static final int APPLICATION_COSE_KEY = 101;
    public static final int APPLICATION_COSE_KEY_SET = 102;
    //RFC6902
    public static final int APPLICATION_JSON_PATCH_JSON = 51;
    //RFC7396
    public static final int APPLICATION_MERGE_PATCH_JSON = 52;
    //RFC7049
    public static final int APPLICATION_CBOR = 60;
    //RFC7390
    public static final int APPLICATION_COAP_GROUP_JSON = 256;
    //RFC8428
    public static final int APPLICATION_SENML_JSON = 110;
    public static final int APPLICATION_SENSML_JSON = 111;
    public static final int APPLICATION_SENML_CBOR = 112;
    public static final int APPLICATION_SENSML_CBOR = 113;
    public static final int APPLICATION_SENML_EXI = 114;
    public static final int APPLICATION_SENSML_EXI = 115;
    public static final int APPLICATION_SENML_XML = 310;
    public static final int APPLICATION_SENSML_XML = 311;


    static final Map<Integer, String> MEDIA_TYPE_MAP = new HashMap<>();

    static {
        MEDIA_TYPE_MAP.put(TEXT_PLAIN, "text/plain");
        MEDIA_TYPE_MAP.put(APPLICATION_XML, "application/xml");
        MEDIA_TYPE_MAP.put(APPLICATION_OCTET_STREAM, "application/octet-stream");
        MEDIA_TYPE_MAP.put(APPLICATION_EXI, "application/exi");
        MEDIA_TYPE_MAP.put(APPLICATION_JSON, "application/json");
        MEDIA_TYPE_MAP.put(APPLICATION_LINK_FORMAT, "application/link-format");
        //OMA LwM2M
        MEDIA_TYPE_MAP.put(APPLICATION_LWM2M_TLV, "application/vnd.oma.lwm2m+tlv");
        MEDIA_TYPE_MAP.put(APPLICATION_LWM2M_JSON, "application/vnd.oma.lwm2m+json");
        //RFC8152
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_ENCRYPT0, "application/cose; cose-type=\"cose-encrypt0\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_MAC0, "application/cose; cose-type=\"cose-mac0\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_SIGN1, "application/cose; cose-type=\"cose-sign1\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_ENCRYPT, "application/cose; cose-type=\"cose-encrypt\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_MAC, "application/cose; cose-type=\"cose-mac\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_SIGN, "application/cose; cose-type=\"cose-sign\"");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_KEY, "application/cose-key");
        MEDIA_TYPE_MAP.put(APPLICATION_COSE_KEY_SET, "application/cose-key-set");
        //RFC6902
        MEDIA_TYPE_MAP.put(APPLICATION_JSON_PATCH_JSON, "application/json-patch+json");
        //RFC7396
        MEDIA_TYPE_MAP.put(APPLICATION_MERGE_PATCH_JSON, "application/merge-patch+json");
        //RFC7049
        MEDIA_TYPE_MAP.put(APPLICATION_CBOR, "application/cbor");
        //RFC7390
        MEDIA_TYPE_MAP.put(APPLICATION_COAP_GROUP_JSON, "application/coap-group+json");
        //RFC8428
        MEDIA_TYPE_MAP.put(APPLICATION_SENML_JSON, "application/senml+json");
        MEDIA_TYPE_MAP.put(APPLICATION_SENSML_JSON, "application/sensml+json");
        MEDIA_TYPE_MAP.put(APPLICATION_SENML_CBOR, "application/senml+cbor");
        MEDIA_TYPE_MAP.put(APPLICATION_SENSML_CBOR, "application/sensml+cbor");
        MEDIA_TYPE_MAP.put(APPLICATION_SENML_EXI, "application/senml-exi");
        MEDIA_TYPE_MAP.put(APPLICATION_SENSML_EXI, "application/sensml-exi");
        MEDIA_TYPE_MAP.put(APPLICATION_SENML_XML, "application/senml+xml");
        MEDIA_TYPE_MAP.put(APPLICATION_SENSML_XML, "application/sensml+xml");

    }

    /**
     * Converts CoAP content format to a MIME media type.
     *
     * @param contentFormat content format
     * @return MIME media type or null if could not convert
     */
    public static String contentFormatToString(Integer contentFormat) {
        if (contentFormat == null) {
            return null;
        }
        return MEDIA_TYPE_MAP.get(contentFormat);
    }

    /**
     * Parses MIME media type to CoAP content format. If can not find
     * matching content format, null is returned.
     *
     * @param contentType MIME content type
     * @return CoAP content format
     */
    public static Integer parseContentFormat(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (Map.Entry<Integer, String> entry : MEDIA_TYPE_MAP.entrySet()) {
            if (entry.getValue().equals(contentType)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
