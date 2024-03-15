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
package com.mbed.coap.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class CodeTest {

    @Test
    public void test() throws Exception {
        assertEquals(412, Code.C412_PRECONDITION_FAILED.getHttpCode());


        assertEquals("504", Code.C504_GATEWAY_TIMEOUT.codeToString());
        //run through all
        for (Code code : Code.values()) {
            code.codeToString();
        }


        assertEquals(Code.C505_PROXYING_NOT_SUPPORTED, Code.valueOf(5, 5));
        assertEquals(null, Code.valueOf(6, 0));
    }

    @Test
    public void testSignaling() {
        assertTrue(Code.C701_CSM.isSignaling());
        assertTrue(Code.C702_PING.isSignaling());
        assertTrue(Code.C703_PONG.isSignaling());
        assertTrue(Code.C704_RELEASE.isSignaling());
        assertTrue(Code.C705_ABORT.isSignaling());

        assertFalse(Code.C203_VALID.isSignaling());
        assertFalse(Code.C405_METHOD_NOT_ALLOWED.isSignaling());
        assertFalse(Code.C503_SERVICE_UNAVAILABLE.isSignaling());
    }

    @Test
    public void testIsError() {
        assertTrue(Code.C400_BAD_REQUEST.isError());
        assertTrue(Code.C401_UNAUTHORIZED.isError());
        assertTrue(Code.C402_BAD_OPTION.isError());
        assertTrue(Code.C403_FORBIDDEN.isError());
        assertTrue(Code.C404_NOT_FOUND.isError());
        assertTrue(Code.C405_METHOD_NOT_ALLOWED.isError());
        assertTrue(Code.C406_NOT_ACCEPTABLE.isError());
        assertTrue(Code.C408_REQUEST_ENTITY_INCOMPLETE.isError());
        assertTrue(Code.C409_CONFLICT.isError());
        assertTrue(Code.C412_PRECONDITION_FAILED.isError());
        assertTrue(Code.C413_REQUEST_ENTITY_TOO_LARGE.isError());
        assertTrue(Code.C415_UNSUPPORTED_MEDIA_TYPE.isError());
        assertTrue(Code.C422_UNPROCESSABLE_ENTITY.isError());
        assertTrue(Code.C500_INTERNAL_SERVER_ERROR.isError());
        assertTrue(Code.C501_NOT_IMPLEMENTED.isError());
        assertTrue(Code.C502_BAD_GATEWAY.isError());
        assertTrue(Code.C503_SERVICE_UNAVAILABLE.isError());
        assertTrue(Code.C504_GATEWAY_TIMEOUT.isError());
        assertTrue(Code.C505_PROXYING_NOT_SUPPORTED.isError());

        assertFalse(Code.C203_VALID.isError());
        assertFalse(Code.C231_CONTINUE.isError());
        assertFalse(Code.C701_CSM.isError());
        assertFalse(Code.C702_PING.isError());
    }

    @Test
    public void testIsSuccess() {
        assertTrue(Code.C201_CREATED.isSuccess());
        assertTrue(Code.C202_DELETED.isSuccess());
        assertTrue(Code.C203_VALID.isSuccess());
        assertTrue(Code.C204_CHANGED.isSuccess());
        assertTrue(Code.C205_CONTENT.isSuccess());
        assertTrue(Code.C231_CONTINUE.isSuccess());

        assertFalse(Code.C405_METHOD_NOT_ALLOWED.isSuccess());
        assertFalse(Code.C412_PRECONDITION_FAILED.isSuccess());
        assertFalse(Code.C701_CSM.isSuccess());
        assertFalse(Code.C705_ABORT.isSuccess());
        assertFalse(Code.C500_INTERNAL_SERVER_ERROR.isSuccess());
        assertFalse(Code.C503_SERVICE_UNAVAILABLE.isSuccess());
    }
}