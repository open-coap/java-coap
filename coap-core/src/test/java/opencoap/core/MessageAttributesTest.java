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

import static java.util.Collections.emptySet;
import static opencoap.core.MessageAttributes.EMPTY;
import static org.assertj.core.util.Sets.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

public class MessageAttributesTest {

    private AttributeKey<String> DUMMY_KEY = AttributeKey.optional("DUMMY_KEY");
    private AttributeKey<String> DUMMY_KEY2 = AttributeKey.defaulted("DUMMY_KEY2", "na");
    private AttributeKey<String> DUMMY_KEY3 = AttributeKey.optional("DUMMY_KEY3");

    @Test
    void test() {
        MessageAttributes trans = MessageAttributes.of(DUMMY_KEY, "perse");
        assertEquals("perse", trans.get(DUMMY_KEY));
        assertEquals("na", trans.get(DUMMY_KEY2));

        trans = trans.with(DUMMY_KEY2, "afds");
        assertEquals("perse", trans.get(DUMMY_KEY));
        assertEquals("afds", trans.get(DUMMY_KEY2));
    }

    @Test
    void merge() {
        MessageAttributes ctx1 = MessageAttributes.of(DUMMY_KEY, "111");
        MessageAttributes ctx2 = MessageAttributes.of(DUMMY_KEY2, "222");

        MessageAttributes ctx3 = ctx1.with(ctx2);

        assertEquals("111", ctx3.get(DUMMY_KEY));
        assertEquals("222", ctx3.get(DUMMY_KEY2));
        assertEquals(set(DUMMY_KEY, DUMMY_KEY2), ctx3.keys());
    }

    @Test
    void mergeWithEmpty() {
        MessageAttributes ctx1 = MessageAttributes.of(DUMMY_KEY, "111");

        assertEquals(ctx1, ctx1.with(EMPTY));
        assertEquals(ctx1, EMPTY.with(ctx1));
        assertEquals(set(DUMMY_KEY), ctx1.keys());
    }

    @Test
    void mergeAndOverWrite() {
        MessageAttributes ctx1 = MessageAttributes.of(DUMMY_KEY, "111").with(DUMMY_KEY2, "222");
        MessageAttributes ctx2 = MessageAttributes.of(DUMMY_KEY, "aaa").with(DUMMY_KEY3, "333");

        MessageAttributes ctx3 = ctx1.with(ctx2);

        assertEquals("aaa", ctx3.get(DUMMY_KEY));
        assertEquals("222", ctx3.get(DUMMY_KEY2));
        assertEquals("333", ctx3.get(DUMMY_KEY3));

        assertEquals(set(DUMMY_KEY, DUMMY_KEY2, DUMMY_KEY3), ctx3.keys());
    }

    @Test
    void empty() {
        MessageAttributes trans = EMPTY;
        assertNull(trans.get(DUMMY_KEY));
        assertEquals("default-val", trans.getOrDefault(DUMMY_KEY2, "default-val"));
        assertEquals("na", trans.get(DUMMY_KEY2));

        assertEquals(EMPTY, EMPTY.with(EMPTY));
    }

    @Test
    public void equalsAndHashTest() throws Exception {
        EqualsVerifier.forClass(MessageAttributes.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withPrefabValues(MessageAttributes.class, EMPTY, MessageAttributes.of(MessageAttributes.NON_CONFIRMABLE, true))
                .verify();
    }

    @Test
    public void equalsAndHashKeys() {
        assertEquals(DUMMY_KEY, DUMMY_KEY);

        assertNotEquals(DUMMY_KEY, DUMMY_KEY2);
        assertNotEquals(DUMMY_KEY, DUMMY_KEY3);

        assertNotEquals(AttributeKey.optional("same"), AttributeKey.optional("same"));
    }

    @Test
    void requiredKey() {
        AttributeKey<String> requiredKey = AttributeKey.required("REQUIRED_KEY");

        assertEquals("val", MessageAttributes.of(requiredKey, "val").get(requiredKey));
        assertEquals("default-val", EMPTY.getOrDefault(requiredKey, "default-val"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> MessageAttributes.of(DUMMY_KEY, "111").get(requiredKey));
        assertEquals("Missing required attribute: REQUIRED_KEY", ex.getMessage());
    }

    @Test
    void defaultedKeyRequiresDefaultValue() {
        assertThrows(NullPointerException.class, () -> AttributeKey.defaulted("KEY", null));
    }

    @Test
    void keyToString() {
        assertEquals("DUMMY_KEY", DUMMY_KEY.toString());
        assertEquals("DUMMY_KEY", DUMMY_KEY.getName());
    }

    @Test
    void emptyHasNoKeys() {
        assertEquals(emptySet(), EMPTY.keys());
    }

    @Test
    void toStringShowsEffectiveValues() {
        AttributeKey<byte[]> bytesKey = AttributeKey.optional("BYTES_KEY");

        assertEquals("MessageAttributes[]", EMPTY.toString());
        assertEquals("MessageAttributes[DUMMY_KEY=111]", MessageAttributes.of(DUMMY_KEY, "111").toString());
        assertEquals("MessageAttributes[DUMMY_KEY=aaa, DUMMY_KEY2=222]",
                MessageAttributes.of(DUMMY_KEY, "111").with(DUMMY_KEY2, "222").with(DUMMY_KEY, "aaa").toString()
        );
        assertEquals("MessageAttributes[BYTES_KEY=0102]", MessageAttributes.of(bytesKey, new byte[]{1, 2}).toString());
    }

}
