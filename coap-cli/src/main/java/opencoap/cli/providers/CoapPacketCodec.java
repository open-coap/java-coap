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
package opencoap.cli.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import opencoap.codec.CoapPacket;
import opencoap.codec.CoapTcpPacketSerializer;
import opencoap.core.CoapException;

public interface CoapPacketCodec {

    CoapPacketCodec UDP = new CoapPacketCodec() {
        @Override
        public void serialize(OutputStream outputStream, CoapPacket coapPacket) {
            opencoap.codec.CoapSerializer.serialize(coapPacket, outputStream);
        }

        @Override
        public CoapPacket deserialize(InputStream inputStream, InetSocketAddress sourceAddress) throws CoapException {
            return opencoap.codec.CoapSerializer.deserialize(sourceAddress, inputStream);
        }
    };

    CoapPacketCodec TCP = new CoapPacketCodec() {
        @Override
        public void serialize(OutputStream outputStream, CoapPacket coapPacket) throws CoapException, IOException {
            CoapTcpPacketSerializer.writeTo(outputStream, coapPacket);
        }

        @Override
        public CoapPacket deserialize(InputStream inputStream, InetSocketAddress sourceAddress) throws CoapException, IOException {
            return CoapTcpPacketSerializer.deserialize(sourceAddress, inputStream);
        }
    };

    void serialize(OutputStream outputStream, CoapPacket coapPacket) throws CoapException, IOException;

    CoapPacket deserialize(InputStream inputStream, InetSocketAddress sourceAddress) throws CoapException, IOException;
}
