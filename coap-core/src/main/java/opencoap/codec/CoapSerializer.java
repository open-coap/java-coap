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
package opencoap.codec;

import static opencoap.codec.PacketUtils.read16;
import static opencoap.codec.PacketUtils.read8;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import opencoap.core.CoapException;
import opencoap.core.CoapOptions;
import opencoap.core.Code;
import opencoap.core.MessageType;
import opencoap.core.Method;
import opencoap.core.Opaque;

public class CoapSerializer {
    public static final int PAYLOAD_MARKER = 0xFF;

    /**
     * Serialize CoAP message
     *
     * @param coapPacket CoAP packet object
     * @return serialized data
     */
    public static byte[] serialize(CoapPacket coapPacket) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        serialize(coapPacket, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Writes serialized CoAP packet to given OutputStream.
     *
     * @param outputStream output stream
     */
    public static void serialize(CoapPacket coap, OutputStream outputStream) {
        try {
            int tempByte;

            tempByte = (0x3 & 1) << 6;                                 // Version
            tempByte |= (0x3 & coap.getMessageType().ordinal()) << 4;  // Transaction Message Type
            tempByte |= coap.getToken().size() & 0xF;                  // Token length

            outputStream.write(tempByte);
            writeCode(outputStream, coap);

            outputStream.write(0xFF & (coap.getMessageId() >> 8));
            outputStream.write(0xFF & coap.getMessageId());

            //token
            coap.getToken().writeTo(outputStream);

            // options
            serializeOptions(coap.headers(), outputStream);

            //payload
            if (coap.getPayload().nonEmpty()) {
                outputStream.write(PAYLOAD_MARKER);
                coap.getPayload().writeTo(outputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    public static Code writeCode(OutputStream os, CoapPacket coapPacket) throws IOException {
        Code code = coapPacket.getCode();
        Method method = coapPacket.getMethod();

        if (code != null && method != null) {
            throw new IllegalStateException("Forbidden operation: 'code' and 'method' use at a same time");
        }
        if (code != null) {
            os.write(code.getCoapCode());
        } else if (method != null) {
            os.write(method.getCode());
        } else { //no code or method used
            os.write(0);
        }
        return code;
    }

    /**
     * Reads CoAP packet from raw data.
     *
     * @param remoteAddress remote address
     * @param rawData data
     * @return CoapPacket instance
     * @throws CoapException if can not parse
     */
    public static CoapPacket deserialize(InetSocketAddress remoteAddress, byte[] rawData) throws CoapException {
        return deserialize(remoteAddress, rawData, rawData.length);
    }

    /**
     * Reads CoAP packet from raw data.
     *
     * @param remoteAddress remote address
     * @param rawData data
     * @param length data length
     * @return CoapPacket instance
     * @throws CoapException if can not parse
     */
    public static CoapPacket deserialize(InetSocketAddress remoteAddress, byte[] rawData, int length) throws CoapException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rawData, 0, length);
        return deserialize(remoteAddress, inputStream);
    }

    /**
     * De-serialize CoAP message from input stream.
     *
     * @param remoteAddress remote address
     * @param input input stream
     * @return CoapPacket instance
     * @throws CoapException if can not parse
     */
    public static CoapPacket deserialize(InetSocketAddress remoteAddress, InputStream input) throws CoapException {
        InputStream inputStream = EofInputStream.wrap(input);

        CoapPacket coap = new CoapPacket(remoteAddress);
        try {
            int tempByte = inputStream.read();      //first byte

            int version = (byte) ((tempByte & 0xC0) >> 6);
            if (version != 1) {
                throw new CoapException("CoAP version %s not supported", version);
            }

            coap.setMessageType(MessageType.valueOf((tempByte >> 4) & 0x3));

            byte tokenLen = (byte) (tempByte & 0x0F);
            if (tokenLen > 8) {
                throw new CoapException("Wrong TOKEN value, size should be within range 0-8");
            }

            tempByte = inputStream.read();         //second byte
            if (tempByte >= 1 && tempByte <= 10) {
                //method code
                coap.setMethod(Method.valueOf(tempByte));
            } else {
                coap.setCode(Code.valueOf(tempByte));
            }

            int messageId = inputStream.read() << 8;
            messageId = messageId | inputStream.read();
            coap.setMessageId(messageId);

            //token
            coap.setToken(Opaque.read(inputStream, tokenLen));

            //read headers
            CoapOptions options = new CoapOptions();
            boolean hasPayloadMarker = deserializeOptions(options, inputStream);
            coap.setHeaderOptions(options);

            //read payload
            if (hasPayloadMarker) {
                int plLen = inputStream.available();
                coap.setPayload(Opaque.read(inputStream, plLen));
            }

            return coap;

        } catch (IOException | IllegalArgumentException ex) {
            throw new CoapException(ex);
        }
    }
    /**
     * Writes serialized CoAP header options to given OutputStream.
     *
     * @param options options to serialize
     * @param os output stream
     */
    public static void serializeOptions(CoapOptions options, OutputStream os) throws IOException {
        List<RawOption> list = options.getRawOptions();
        Collections.sort(list);

        int lastOptNumber = 0;
        for (RawOption opt : list) {
            for (Opaque optValue : opt.optValues) {
                int delta = opt.optNumber - lastOptNumber;
                lastOptNumber = opt.optNumber;
                if (delta > 0xFFFF + 269) {
                    throw new IllegalArgumentException("Delta with size: " + delta + " is not supported [option number: " + opt.optNumber + "]");
                }
                int len = optValue.size();
                if (len > 0xFFFF + 269) {
                    throw new IllegalArgumentException("Header size: " + len + " is not supported [option number: " + opt.optNumber + "]");
                }
                writeOptionHeader(delta, len, os);
                optValue.writeTo(os);
            }
        }
    }

    @SuppressWarnings("PMD.NPathComplexity")
    static void writeOptionHeader(int delta, int len, OutputStream os) throws IOException {
        //first byte
        int tempByte;
        if (delta <= 12) {
            tempByte = delta << 4;
        } else if (delta < 269) {
            tempByte = 13 << 4;
        } else {
            tempByte = 14 << 4;
        }
        if (len <= 12) {
            tempByte |= len;
        } else if (len < 269) {
            tempByte |= 13;
        } else {
            tempByte |= 14;
        }
        os.write(tempByte);

        //extended option delta
        if (delta > 12 && delta < 269) {
            os.write(delta - 13);
        } else if (delta >= 269) {
            os.write((0xFF00 & (delta - 269)) >> 8);
            os.write(0x00FF & (delta - 269));
        }
        //extended len
        if (len > 12 && len < 269) {
            os.write(len - 13);
        } else if (len >= 269) {
            os.write((0xFF00 & (len - 269)) >> 8);
            os.write(0x00FF & (len - 269));
        }
    }

    /**
     * De-serializes CoAP header options into given options, reading all available data.
     *
     * @return true when a payload marker was found, meaning a payload follows
     */
    public static boolean deserializeOptions(CoapOptions options, InputStream is) throws IOException, CoapMessageFormatException {
        return deserializeOptions(options, is, is.available()) != 0;
    }

    /**
     * De-serializes CoAP header options into given options. Returns left stream/data length if
     * PayloadMarker was found or zero if no payload present.
     * If no payload marker found but still data present - CoapMessageException is thrown.
     */
    public static int deserializeOptions(CoapOptions options, InputStream is, int availableBytes) throws IOException, CoapMessageFormatException {

        int availableInternal = availableBytes;
        int headerOptNum = 0;
        // olesmi:
        // if we have whole packet (UDP, DTLS) we should read till end of stream, expecting whole packet contained in stream
        // if we have TCP stream - we should try to read withing provided packetLen (optionsAndPayloadLen). If stream ends
        // here we should throw EOFException (from underlying StrictInputStream) or should throw NotEnoughDataException if we
        // are waiting for more data. While querying is.available() if stream is closed, unfortunately IOException will be
        // thrown instead of EOFException (implementation for SocketInputStream)
        while (availableInternal > 0) {
            int hdrByte = read8(is);
            availableInternal--;

            if (hdrByte == PAYLOAD_MARKER) {
                return availableInternal;
            }
            int delta = hdrByte >> 4;
            int len = 0xF & hdrByte;

            if (delta == 15 || len == 15) {
                throw new CoapMessageFormatException("Unexpected delta or len value in option header after optNum: " + headerOptNum);
            }
            if (delta == 13) {
                delta += read8(is);
                availableInternal--;
            } else if (delta == 14) {
                delta = read16(is) + 269;
                availableInternal -= 2;
            }
            if (len == 13) {
                len += read8(is);
                availableInternal--;
            } else if (len == 14) {
                len = read16(is) + 269;
                availableInternal -= 2;
            }
            headerOptNum += delta;
            Opaque headerOptData = Opaque.read(is, len);
            availableInternal -= len;
            if (options.isTextOption(headerOptNum) && headerOptData.hasControlChars()) {
                // deliberately without the value itself, it lands in a log
                throw new CoapMessageFormatException("Control character in option: " + headerOptNum);
            }
            options.put(headerOptNum, headerOptData);
        }
        if (availableInternal < 0) {
            throw new CoapMessageFormatException("No payload marker found and options read more that were available");
        }
        return availableInternal;

    }
}
