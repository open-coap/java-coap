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

import static opencoap.core.CoapOptions.hasNoCacheKey;
import static opencoap.core.CoapOptions.isCritical;
import static opencoap.core.CoapOptions.isUnsave;
import static opencoap.core.Opaque.decodeHex;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import opencoap.codec.CoapMessageFormatException;
import opencoap.codec.DataConvertingUtility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CoapOptionsTest {

    @Test
    public void testEmpty() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(0, baos.size());
    }

    @Test
    public void headerWithLargeDelta() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setProxyUri("/testuri"); //35

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        byte[] expected = new byte[]{(byte) 0xD8, 0x16, '/', 't', 'e', 's', 't', 'u', 'r', 'i'};
        assertArrayEquals(expected, baos.toByteArray());
        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(expected));
        assertEquals(hdr.getProxyUri(), hdr2.getProxyUri());
        assertEquals(hdr.getProxyScheme(), hdr2.getProxyScheme());

        //larger delta
        hdr = new CoapOptions();
        hdr.put(300, Opaque.of("test"));

        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        expected = new byte[]{(byte) 0xE4, 0x00, 0x1F, 't', 'e', 's', 't'};
        assertArrayEquals(expected, baos.toByteArray());
    }

    @Test
    public void testMultipleHeaders() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setUriPath("/test/uri/path");
        //hdr.setToken(CoapOptions.convertVariableUInt(123456));
        hdr.setContentFormat(1);
        hdr.setEtag(Opaque.variableUInt((56789)));
        hdr.setLocationPath("/location/path");
        hdr.setProxyUri("/proxy/uri");
        hdr.setProxyScheme("coap");
        hdr.setUriHost("uri-host");
        hdr.setUriPort(5683);
        hdr.setUriQueryList("par1=dupa", "par2=dupa2");
        hdr.put(36, Opaque.variableUInt((1357)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));

        System.out.println(hdr);
        System.out.println(hdr2);
        assertTrue(hdr.equals(hdr2));

        // duplicate
        assertEquals(hdr, hdr.duplicate());
    }

    @Test
    public void testWithEmptyLocation() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        //hdr.setUriPath("/test/uri/path");
        hdr.setLocationPath("");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));

        System.out.println(hdr);
        System.out.println(hdr2);
        assertTrue(hdr.equals(hdr2));
    }

    @Test
    public void testWithAccept() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setAccept(123);
        CoapOptions hdr2 = deserialize(serialize(hdr));

        System.out.println(hdr.toString());
        System.out.println(hdr2.toString());
        assertEquals(123, hdr2.getAccept().intValue());
        assertEquals(hdr, hdr2);

        hdr.setAccept(null);
        hdr2 = deserialize(serialize(hdr));
        assertEquals(hdr, hdr2);
    }

    @Test
    public void should_fail_when_illegal_accept_value() {
        CoapOptions hdr = new CoapOptions();

        assertThatThrownBy(() -> hdr.setAccept(-1)).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hdr.setAccept(0x10000)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWithPath() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setUriPath("/path2");
        hdr.setLocationPath("");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));

        System.out.println(hdr);
        System.out.println(hdr2);
        assertTrue(hdr.equals(hdr2));
    }

    @Test
    public void testMultipleExHeaders() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setBlock1Req(new BlockOption(2, BlockSize.S_16, true));
        hdr.setBlock2Res(new BlockOption(4, BlockSize.S_1024, false));
        hdr.setObserve(4321);
        hdr.setEcho(decodeHex("0102030405060708090a"));
        hdr.setRequestTag(Opaque.of("tag-0001"));
        hdr.setCorrelationTag("RequestId1234");

        CoapOptions hdr2 = deserialize(serialize(hdr));

        System.out.println(hdr);
        System.out.println(hdr2);
        assertEquals(hdr, hdr2);

        // duplicate
        assertEquals(hdr, hdr.duplicate());
    }

    @Test
    public void testWithLargeOptionNumbers() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.put(1000, Opaque.variableUInt(123456));
        hdr.put(12000, Opaque.variableUInt(98));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        CoapOptions hdr2 = new CoapOptions();
        System.out.println(Arrays.toString(baos.toByteArray()));
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));
    }

    @Test
    public void largeOptionValues() throws IOException, CoapException {
        Opaque OPT_VAL1 = Opaque.of("123456789 1234567890"); //20
        Opaque OPT_VAL2 = Opaque.of("123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "); //300
        Opaque OPT_VAL3 = Opaque.of("123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "); //600
        Opaque OPT_VAL4 = Opaque.of("123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "
                + "123456789 1234567890123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 "); //900

        CoapOptions hdr = new CoapOptions();
        hdr.put(101, OPT_VAL1);
        hdr.put(102, OPT_VAL2);
        hdr.put(103, OPT_VAL3);
        hdr.put(104, OPT_VAL4);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);

        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));
        System.out.println(hdr);
        System.out.println(hdr2);
        assertEquals(hdr, hdr2);

        assertEquals(OPT_VAL1, hdr2.getCustomOption(101));
        assertEquals(OPT_VAL2, hdr2.getCustomOption(102));
        assertEquals(OPT_VAL3, hdr2.getCustomOption(103));
        assertEquals(OPT_VAL4, hdr2.getCustomOption(104));
        assertNull(hdr2.getCustomOption(999));
    }


    @Test
    public void specialHeaderValueSizes() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setUriPath("/123456789012"); //12

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(1 + 12, baos.size());
        assertEquals((byte) 0xBC, baos.toByteArray()[0]);    //header
        assertEquals((byte) '1', baos.toByteArray()[1]);     //first byte of option value

        hdr.setUriPath("/1234567890123"); //13

        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(2 + 13, baos.size());
        assertEquals((byte) 0xBD, baos.toByteArray()[0]);    //header
        assertEquals((byte) 0x00, baos.toByteArray()[1]);    //extended len
        assertEquals((byte) '1', baos.toByteArray()[2]);     //first byte of option value

        hdr.setUriPath("/1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890123456789012345678"); //268

        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(2 + 268, baos.size());
        assertEquals((byte) 0xBD, baos.toByteArray()[0]);    //header
        assertEquals((byte) 0xFF, baos.toByteArray()[1]);    //extended len
        assertEquals((byte) '1', baos.toByteArray()[2]);     //first byte of option value

        hdr.setUriPath("/1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567890123456789"); //269

        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(3 + 269, baos.size());
        assertEquals((byte) 0xBE, baos.toByteArray()[0]);    //header
        assertEquals((byte) 0x00, baos.toByteArray()[1]);    //extended len
        assertEquals((byte) 0x00, baos.toByteArray()[2]);    //extended len
        assertEquals((byte) '1', baos.toByteArray()[3]);     //first byte of option value
    }

    @Test
    public void testWithLargeOptionAmount() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setUriPath("/1/2/3");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(6, baos.size());

        hdr.setUriPath("/1/2/3/4/5/6/7/8/9/0/1/2/3/4");
        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(2 * 14, baos.size());
        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(hdr, hdr2);

        hdr.setUriPath("/1/2/3/4/5/6/7/8/9/0/1/2/3/4/5");
        baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        assertEquals(2 * 15, baos.size());
        hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(hdr, hdr2);

    }

    @Test
    public void testEaquals() {
        CoapOptions hdr1 = new CoapOptions();
        hdr1.setContentFormat(1);
        hdr1.setUriPath("/test/uri");

        CoapOptions hdr2 = new CoapOptions();
        hdr2.setContentFormat(1);
        hdr2.setUriPath("/test/uri");

        assertTrue(hdr1.equals(hdr2));
        assertEquals(hdr1.hashCode(), hdr2.hashCode());

        CoapOptions hdr3 = new CoapOptions();
        hdr3.setContentFormat(1);
        hdr3.setUriPath("/test/uri3");

        CoapOptions hdr4 = new CoapOptions();
        hdr4.setContentFormat(1);
        hdr4.setUriPath("/test/uri");

        assertFalse(hdr3.equals(hdr4));
        assertNotEquals(hdr3.hashCode(), hdr4.hashCode());

        hdr3.setUriPath("/test/uri");
        hdr3.setObserve(1234);
        hdr4.setObserve(4321);
        assertFalse(hdr3.equals(hdr4));

        hdr3.setObserve(1234);
        hdr4.setObserve(1234);
        assertTrue(hdr3.equals(hdr4));

        hdr3.setCorrelationTag("1234");
        hdr4.setCorrelationTag("12345");
        assertNotEquals(hdr3.hashCode(), hdr4.hashCode());
        assertFalse(hdr3.equals(hdr4));

        hdr3.setCorrelationTag("1234");
        hdr4.setCorrelationTag("1234");
        assertTrue(hdr3.equals(hdr4));
    }

    @Test
    public void testIllegalLocationPath() {
        CoapOptions hdr = new CoapOptions();
        assertThrows(IllegalArgumentException.class, () ->
                hdr.setLocationPath(".")
        );
    }

    @Test
    public void testSizeOption() throws IOException, CoapException {
        CoapOptions hdr = new CoapOptions();
        hdr.setSize1(3211);

        CoapOptions hdr2 = deserialize(serialize(hdr));
        assertEquals(hdr, hdr2);
        assertEquals((Integer) 3211, hdr2.getSize1());

        hdr2.setSize1(3212);
        assertFalse(hdr.equals(hdr2));
        hdr.setSize1(null);
        assertFalse(hdr.equals(hdr2));
        hdr2.setSize1(null);
        assertTrue(hdr.equals(hdr2));
    }

    @Test
    public void malformedHeaderWithIllegalDelta() throws IOException, CoapMessageFormatException {
        CoapOptions hdr = new CoapOptions();
        assertThrows(CoapMessageFormatException.class, () ->
                hdr.deserialize(new ByteArrayInputStream(new byte[]{(byte) 0xF3}))
        );
    }

    @Test
    public void split() {
        assertArrayEquals(new String[]{"", "3", "", ""}, DataConvertingUtility.split("/3//", '/'));
        assertArrayEquals(new String[]{"", "3", "", "7"}, DataConvertingUtility.split("/3//7", '/'));
        assertArrayEquals(new String[]{"", "3", "20", "7"}, DataConvertingUtility.split("/3/20/7", '/'));

        assertArrayEquals("/1/2/3".split("/"), DataConvertingUtility.split("/1/2/3", '/'));
        assertArrayEquals("/1//3".split("/"), DataConvertingUtility.split("/1//3", '/'));
        assertArrayEquals("/1/432fsdfs/3fds".split("/"), DataConvertingUtility.split("/1/432fsdfs/3fds", '/'));
        assertArrayEquals("boo:and:foo".split("x"), DataConvertingUtility.split("boo:and:foo", 'x'));

    }

    @Test
    public void uriPath_withMultipleEmptyPathSegments() throws Exception {
        CoapOptions hdr = new CoapOptions();
        hdr.setUriPath("/3//");
        CoapOptions hdr2 = deserialize(serialize(hdr));
        assertEquals(hdr, hdr2);
        assertEquals("/3//", hdr2.getUriPath());
    }

    @Test
    public void criticalOptTest() throws Exception {
        CoapOptions h = new CoapOptions();
        assertFalse(h.containsUnrecognisedCriticalOption());

        h.put(1000, Opaque.of("foo"));
        assertFalse(h.containsUnrecognisedCriticalOption());

        h.put(1001, Opaque.of("foo"));
        assertTrue(h.containsUnrecognisedCriticalOption());

        h.put(1001, Opaque.of("foo"));
        assertFalse(h.containsUnrecognisedCriticalOption(Collections.singleton(1001)));
    }

    @Test
    public void optionCharacteristics() throws Exception {
        assertTrue(isCritical(CoapOptions.IF_MATCH));
        assertFalse(isUnsave(CoapOptions.IF_MATCH));
        assertFalse(hasNoCacheKey(CoapOptions.IF_MATCH));

        assertFalse(isCritical(CoapOptions.ETAG));
        assertFalse(isUnsave(CoapOptions.ETAG));
        assertFalse(hasNoCacheKey(CoapOptions.ETAG));

        assertTrue(isCritical(CoapOptions.URI_PORT));
        assertTrue(isUnsave(CoapOptions.URI_PORT));
        assertFalse(hasNoCacheKey(CoapOptions.URI_PORT));

        assertFalse(isCritical(CoapOptions.SIZE1));
        assertFalse(isUnsave(CoapOptions.SIZE1));
        assertTrue(hasNoCacheKey(CoapOptions.SIZE1));
    }

    @Test
    public void settingValuesOverRange() throws Exception {
        CoapOptions h = new CoapOptions();

        h.setMaxAge(null);
        assertNull(h.getMaxAge());

        h.setMaxAge(0x1FFFFFFFFL);
        assertEquals(0xFFFFFFFFL, h.getMaxAgeValue());

        assertThatThrownBy(() -> h.setEtag(Opaque.of("123456789"))).isExactlyInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> h.setEtag(new Opaque[]{Opaque.of("123456789")})).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> h.setEtag(new Opaque[]{Opaque.EMPTY})).isExactlyInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> h.setLocationPath(".")).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> h.setLocationPath("..")).isExactlyInstanceOf(IllegalArgumentException.class);

        h.setLocationPath("");
        assertNull(h.getLocationPath());

        assertThatThrownBy(() -> h.setUriPath("no-leading-slash")).isExactlyInstanceOf(IllegalArgumentException.class);

        h.setUriQueryList();
        assertNull(h.getUriQueryEncoded());
        assertTrue(h.getUriQueryList().isEmpty());
    }

    @Test
    public void failWhenTooLargeToSerialize() throws Exception {
        CoapOptions h = new CoapOptions();
        h.put(100, new Opaque(new byte[65805]));
        assertThrows(IllegalArgumentException.class, () ->
                h.serialize(Mockito.mock(OutputStream.class))
        );
    }

    @Test
    public void failWhenTooLargeDeltaToSerialize() throws Exception {
        CoapOptions h = new CoapOptions();
        h.setIfNonMatch(false);
        h.put(65805, new Opaque(new byte[1]));
        assertThrows(IllegalArgumentException.class, () ->
                h.serialize(Mockito.mock(OutputStream.class))
        );
    }

    @Test
    public void failToDeserializeWithMalformedData() throws Exception {

        assertThatThrownBy(() -> new CoapOptions().deserialize(new ByteArrayInputStream(new byte[]{(byte) 0xf2})))
                .isExactlyInstanceOf(CoapMessageFormatException.class);

        assertThatThrownBy(() -> new CoapOptions().deserialize(new ByteArrayInputStream(new byte[]{0x3f})))
                .isExactlyInstanceOf(CoapMessageFormatException.class);

    }

    @Test
    public void equalsAndHashTest() throws Exception {
        EqualsVerifier.forClass(CoapOptions.class).suppress(Warning.NONFINAL_FIELDS).usingGetClass().verify();

        assertFalse(new CoapOptions().equals(null));
    }

    @Test
    void shouldUseQueryWithoutValue() throws CoapException, IOException {
        CoapOptions h = new CoapOptions();
        // when
        h.setUriQueryList("param1=val1", "q", "param2=val2");
        h.setLocationQuery("q");
        CoapOptions h2 = deserialize(serialize(h));

        // then
        assertEquals(Arrays.asList("param1=val1", "q", "param2=val2"), h2.getUriQueryList());
        assertEquals("", h2.getUriQueryMap().get("q"));

        assertEquals("?param1=val1&q&param2=val2 Loc:?q", h2.toString());
    }

    @Test
    void shouldPercentEncodeUriQueryPerRfc7252() {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("filter=a&b", "note=hello world", "p=100%"));

        // '&' inside a value is escaped, so the joined result is unambiguous
        assertEquals("filter=a%26b&note=hello%20world&p=100%25", h.getUriQueryEncoded());
    }

    @Test
    void shouldNotEncodeSubDelimsInUriQuery() {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("current_version=1.0.0+sha", "a=x,y;z", "b=!$'()*"));

        // sub-delims other than '&', plus ':' '@' '/' '?', are legal unescaped in a CoAP URI query
        assertEquals("current_version=1.0.0+sha&a=x,y;z&b=!$'()*", h.getUriQueryEncoded());
    }

    @Test
    void shouldEncodeUriQueryAsUtf8() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("name=zażółć");

        assertEquals("name=za%C5%BC%C3%B3%C5%82%C4%87", h.getUriQueryEncoded());
    }

    @Test
    void shouldKeepSeparatorForEmptyUriQueryValue() {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("", "a=1"));

        assertEquals("&a=1", h.getUriQueryEncoded());
    }

    @Test
    void shouldRoundTripEmptyUriQueryValue() throws CoapException, IOException {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("", "a=1"));

        CoapOptions h2 = deserialize(serialize(h));

        // a zero length Uri-Query option is its own option, not padding to be dropped
        assertEquals(Arrays.asList("", "a=1"), h2.getUriQueryList());
        assertEquals("&a=1", h2.getUriQueryEncoded());
    }

    @Test
    void shouldReturnNullEncodedUriQueryWhenAbsent() {
        assertNull(new CoapOptions().getUriQueryEncoded());
    }

    @Test
    void shouldRoundTripQueryValueContainingAmpersand() throws CoapException, IOException {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("filter=a&b", "page=1"));

        CoapOptions h2 = deserialize(serialize(h));

        // option boundaries survive, so the '&' stays part of the value
        assertEquals(Arrays.asList("filter=a&b", "page=1"), h2.getUriQueryList());
        assertEquals("a&b", h2.getUriQueryMap().get("filter"));
        assertEquals("1", h2.getUriQueryMap().get("page"));
    }

    @Test
    void shouldNotTruncateQueryValueAtQuestionMark() throws CoapException, IOException {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("redirect=/a?b=c");

        CoapOptions h2 = deserialize(serialize(h));

        assertEquals("/a?b=c", h2.getUriQueryMap().get("redirect"));
    }

    @Test
    void shouldKeepRepeatedQueryNames() throws CoapException, IOException {
        CoapOptions h = new CoapOptions();
        h.setUriQueryList(Arrays.asList("a=1", "a=2"));

        CoapOptions h2 = deserialize(serialize(h));

        assertEquals(Arrays.asList("a=1", "a=2"), h2.getUriQueryList());
        // the map view collapses repeats, keeping the last one
        assertEquals("2", h2.getUriQueryMap().get("a"));
    }

    @Test
    void shouldReturnEmptyQueryViewsWhenNoUriQuery() {
        CoapOptions h = new CoapOptions();

        assertEquals(Collections.emptyList(), h.getUriQueryList());
        assertEquals(Collections.emptyMap(), h.getUriQueryMap());
        assertNull(h.getUriQueryEncoded());
    }

    @Test
    void shouldClearUriQueryWithEmptyList() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("a=1");

        h.setUriQueryList(Collections.emptyList());

        assertEquals(Collections.emptyList(), h.getUriQueryList());
        assertNull(h.getUriQueryEncoded());
    }

    @Test
    void shouldClearUriQueryWithNullList() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("a=1");

        h.setUriQueryList((List<String>) null);

        assertEquals(Collections.emptyList(), h.getUriQueryList());
        assertNull(h.getUriQueryEncoded());
    }

    @Test
    void shouldClearUriQueryWithNullArray() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("a=1");

        h.setUriQueryList((String[]) null);

        assertEquals(Collections.emptyList(), h.getUriQueryList());
        assertNull(h.getUriQueryEncoded());
    }

    @Test
    void shouldSetUriQueryFromVarargs() {
        CoapOptions h = new CoapOptions();

        h.setUriQueryList("a=1", "filter=x&y");

        assertEquals(Arrays.asList("a=1", "filter=x&y"), h.getUriQueryList());
    }

    @Test
    void shouldClearUriQueryWithEmptyVarargs() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("a=1");

        h.setUriQueryList();

        assertEquals(Collections.emptyList(), h.getUriQueryList());
        assertNull(h.getUriQueryEncoded());
    }

    @Test
    void shouldNotShareUriQueryListBetweenDuplicates() {
        CoapOptions h = new CoapOptions();
        h.addUriQuery("a=1");

        CoapOptions copy = h.duplicate();
        copy.addUriQuery("b=2");

        assertEquals(Collections.singletonList("a=1"), h.getUriQueryList());
        assertEquals(Arrays.asList("a=1", "b=2"), copy.getUriQueryList());
    }

    @Test
    void correlationOptionShouldBeElectiveAndSafeToForward() {
        assertFalse(isCritical(CoapOptions.OPEN_COAP_CORRELATION_TAG));
        assertFalse(isUnsave(CoapOptions.OPEN_COAP_CORRELATION_TAG));
        assertFalse(hasNoCacheKey(CoapOptions.OPEN_COAP_CORRELATION_TAG));
    }

    @Test
    void getCustomOption_shouldReturnNull_whenNoCustomOptionsSet() {
        CoapOptions hdr = new CoapOptions();

        assertNull(hdr.getCustomOption(100));
    }

    private static byte[] serialize(CoapOptions hdr) throws IOException, CoapException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        hdr.serialize(baos);
        return baos.toByteArray();
    }

    private static CoapOptions deserialize(byte[] rawData) throws IOException, CoapException {
        CoapOptions hdr2 = new CoapOptions();
        hdr2.deserialize(new ByteArrayInputStream(rawData));
        return hdr2;
    }
}
