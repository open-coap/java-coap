/*
 * Copyright (C) 2022-2026 java-coap contributors (https://github.com/open-coap/java-coap)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class SignalingHeaderOptionsTest {

    @Test
    void duplicate() {
        SignalingHeaderOptions signOpts = new SignalingHeaderOptions(Code.C701_CSM);
        signOpts.putSignalingOptions(SignalingOptions.capabilities(100, true));

        assertEquals(signOpts, signOpts.duplicate());
    }

    @Test
    public void failWhenNotCSMCode() {
        assertThrows(IllegalArgumentException.class, () -> new SignalingHeaderOptions(Code.C205_CONTENT));
    }

    @Test
    public void equalsAndHashTest() throws Exception {
        EqualsVerifier.forClass(SignalingHeaderOptions.class).suppress(Warning.NONFINAL_FIELDS).usingGetClass().verify();
    }

}
