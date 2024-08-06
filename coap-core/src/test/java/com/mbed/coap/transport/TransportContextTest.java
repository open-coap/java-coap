/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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
package com.mbed.coap.transport;

import static com.mbed.coap.transport.TransportContext.EMPTY;
import static org.assertj.core.util.Sets.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;


public class TransportContextTest {

    private TransportContext.Key<String> DUMMY_KEY = new TransportContext.Key<>(null);
    private TransportContext.Key<String> DUMMY_KEY2 = new TransportContext.Key<>("na");
    private TransportContext.Key<String> DUMMY_KEY3 = new TransportContext.Key<>(null);

    @Test
    void test() {
        TransportContext trans = TransportContext.of(DUMMY_KEY, "perse");
        assertEquals("perse", trans.get(DUMMY_KEY));
        assertEquals("na", trans.get(DUMMY_KEY2));

        trans = trans.with(DUMMY_KEY2, "afds");
        assertEquals("perse", trans.get(DUMMY_KEY));
        assertEquals("afds", trans.get(DUMMY_KEY2));
    }

    @Test
    void merge() {
        TransportContext ctx1 = TransportContext.of(DUMMY_KEY, "111");
        TransportContext ctx2 = TransportContext.of(DUMMY_KEY2, "222");

        TransportContext ctx3 = ctx1.with(ctx2);

        assertEquals("111", ctx3.get(DUMMY_KEY));
        assertEquals("222", ctx3.get(DUMMY_KEY2));
        assertEquals(set(DUMMY_KEY, DUMMY_KEY2), ctx3.keys());
    }

    @Test
    void mergeWithEmpty() {
        TransportContext ctx1 = TransportContext.of(DUMMY_KEY, "111");

        assertEquals(ctx1, ctx1.with(EMPTY));
        assertEquals(ctx1, EMPTY.with(ctx1));
        assertEquals(set(DUMMY_KEY), ctx1.keys());
    }

    @Test
    void mergeAndOverWrite() {
        TransportContext ctx1 = TransportContext.of(DUMMY_KEY, "111").with(DUMMY_KEY2, "222");
        TransportContext ctx2 = TransportContext.of(DUMMY_KEY, "aaa").with(DUMMY_KEY3, "333");

        TransportContext ctx3 = ctx1.with(ctx2);

        assertEquals("aaa", ctx3.get(DUMMY_KEY));
        assertEquals("222", ctx3.get(DUMMY_KEY2));
        assertEquals("333", ctx3.get(DUMMY_KEY3));

        assertEquals(set(DUMMY_KEY, DUMMY_KEY2, DUMMY_KEY3), ctx3.keys());
    }

    @Test
    void empty() {
        TransportContext trans = EMPTY;
        assertNull(trans.get(DUMMY_KEY));
        assertEquals("default-val", trans.getOrDefault(DUMMY_KEY2, "default-val"));
        assertEquals("na", trans.get(DUMMY_KEY2));

        assertEquals(EMPTY, EMPTY.with(EMPTY));
    }

    @Test
    public void equalsAndHashTest() throws Exception {
        EqualsVerifier.forClass(TransportContext.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withPrefabValues(TransportContext.class, EMPTY, TransportContext.of(TransportContext.NON_CONFIRMABLE, true))
                .verify();
    }

    @Test
    public void equalsAndHashKeys() {
        assertEquals(DUMMY_KEY, DUMMY_KEY);

        assertNotEquals(DUMMY_KEY, DUMMY_KEY2);
        assertNotEquals(DUMMY_KEY, DUMMY_KEY3);

        assertNotEquals(new TransportContext.Key<String>(null), new TransportContext.Key<String>(null));
    }

}
