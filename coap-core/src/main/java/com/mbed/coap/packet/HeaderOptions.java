/*
 * Copyright (C) 2022-2024 java-coap contributors (https://github.com/open-coap/java-coap)
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

import static com.mbed.coap.utils.Validations.require;
import java.util.List;
import java.util.Objects;

/**
 * Implements CoAP additional header options from
 * - RFC 7959 (Block-Wise Transfers)
 * - draft-ietf-core-observe-09
 * - RFC 9175 (Echo, Request-Tag, and Token Processing)
 * <p>
 * And custom:
 * <pre>
 *    +-----+---+---+---+---+----------------+--------+--------+----------+
 *    | No. | C | U | N | R | Name           | Format | Length | Default  |
 *    +-----+---+---+---+---+----------------+--------+--------+----------+
 *    |29644|   |   |   |   | Correlation-tag| opaque | 0-36   | (none)   |
 *    +-----+---+---+---+---+----------------+--------+--------+----------+
 * </pre>
 */
public class HeaderOptions extends BasicHeaderOptions {

    private static final byte OBSERVE = 6;
    private static final byte BLOCK_1_REQ = 27;
    private static final byte BLOCK_2_RES = 23;
    private static final byte SIZE_2_RES = 28;
    private static final int ECHO = 252;
    private static final int REQUEST_TAG = 292;
    public static final int OPEN_COAP_CORRELATION_TAG = 29644; // open-coap specific option for request tracing
    private Integer observe;
    private BlockOption block1Req;
    private BlockOption block2Res;
    private Integer size2Res;
    private Opaque echo;
    private Opaque requestTag;
    private String correlationTag;

    @Override
    public boolean parseOption(int type, Opaque data) {
        switch (type) {
            case OBSERVE:
                setObserve(data.toInt());
                break;
            case BLOCK_2_RES:
                setBlock2Res(new BlockOption(data));
                break;
            case BLOCK_1_REQ:
                setBlock1Req(new BlockOption(data));
                break;
            case SIZE_2_RES:
                setSize2Res(data.toInt());
                break;
            case ECHO:
                setEcho(data);
                break;
            case REQUEST_TAG:
                setRequestTag(data);
                break;
            case OPEN_COAP_CORRELATION_TAG:
                setCorrelationTag(data.toUtf8String());
                break;
            default:
                return super.parseOption(type, data);

        }
        return true;
    }

    @Override
    List<RawOption> getRawOptions() {
        List<RawOption> l = super.getRawOptions();
        if (observe != null) {
            if (observe == 0) {
                l.add(RawOption.fromEmpty(OBSERVE));
            } else {
                l.add(RawOption.fromUint(OBSERVE, observe.longValue()));
            }
        }
        if (block1Req != null) {
            l.add(new RawOption(BLOCK_1_REQ, new Opaque[]{getBlock1Req().toBytes()}));
        }
        if (block2Res != null) {
            l.add(new RawOption(BLOCK_2_RES, new Opaque[]{getBlock2Res().toBytes()}));
        }
        if (size2Res != null) {
            l.add(RawOption.fromUint(SIZE_2_RES, size2Res.longValue()));
        }
        if (echo != null) {
            l.add(new RawOption(ECHO, echo));
        }
        if (requestTag != null) {
            l.add(new RawOption(REQUEST_TAG, requestTag));
        }
        if (correlationTag != null) {
            l.add(new RawOption(OPEN_COAP_CORRELATION_TAG, Opaque.of(correlationTag)));
        }

        return l;
    }

    @Override
    public void buildToString(StringBuilder sb) {
        super.buildToString(sb);

        if (block1Req != null) {
            sb.append(" block1:").append(block1Req);
        }
        if (block2Res != null) {
            sb.append(" block2:").append(block2Res);
        }
        if (observe != null) {
            sb.append(" obs:").append(observe);
        }
        if (size2Res != null) {
            sb.append(" sz2:").append(size2Res);
        }
        if (echo != null) {
            sb.append(" Echo:").append(echo.toHex());
        }
        if (requestTag != null) {
            sb.append(" Req-tag:").append(requestTag.toHex());
        }
        if (correlationTag != null) {
            sb.append(" Corr-tag:").append(correlationTag);
        }

    }

    /**
     * @return the subsLifetime
     */
    public Integer getObserve() {
        return observe;
    }

    /**
     * Sets observer option value. Allowed value range: 3 bytes.
     *
     * @param observe the subsLifetime to set
     */
    public void setObserve(Integer observe) {
        if (observe != null && (observe < 0 || observe > 0xFFFFFF)) {
            throw new IllegalArgumentException("Illegal observe argument: " + observe);
        }
        this.observe = observe;
    }

    /**
     * @return the request block
     */
    public BlockOption getBlock1Req() {
        return block1Req;
    }

    public BlockOption getBlock2Res() {
        return block2Res;
    }

    public Integer getSize2Res() {
        return size2Res;
    }

    /**
     * @param block the block to set
     */
    public void setBlock1Req(BlockOption block) {
        this.block1Req = block;
    }

    public void setBlock2Res(BlockOption block) {
        this.block2Res = block;
    }

    public void setSize2Res(Integer size2Res) {
        this.size2Res = size2Res;
    }

    public void setEcho(Opaque echo) {
        require(echo == null || echo.size() <= 40);
        this.echo = echo;
    }

    public Opaque getEcho() {
        return echo;
    }

    public void setRequestTag(Opaque requestTag) {
        require(requestTag == null || requestTag.size() <= 8);
        this.requestTag = requestTag;
    }

    public Opaque getRequestTag() {
        return requestTag;
    }

    void setCorrelationTag(String corrTag) {
        require(corrTag == null || corrTag.length() <= 36);
        this.correlationTag = corrTag;
    }

    public String getCorrelationTag() {
        return correlationTag;
    }

    public HeaderOptions duplicate() {
        HeaderOptions opts = new HeaderOptions();
        super.duplicate(opts);

        opts.observe = observe;
        opts.block1Req = block1Req;
        opts.block2Res = block2Res;
        opts.size2Res = size2Res;
        opts.echo = echo;
        opts.requestTag = requestTag;
        opts.correlationTag = correlationTag;

        return opts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        HeaderOptions that = (HeaderOptions) o;

        if (!Objects.equals(correlationTag, that.correlationTag)) {
            return false;
        }
        if (!Objects.equals(echo, that.echo)) {
            return false;
        }
        if (!Objects.equals(requestTag, that.requestTag)) {
            return false;
        }
        if (!Objects.equals(observe, that.observe)) {
            return false;
        }
        if (!Objects.equals(block1Req, that.block1Req)) {
            return false;
        }
        if (!Objects.equals(block2Res, that.block2Res)) {
            return false;
        }

        return Objects.equals(size2Res, that.size2Res);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (correlationTag != null ? correlationTag.hashCode() : 0);
        result = 31 * result + (echo != null ? echo.hashCode() : 0);
        result = 31 * result + (requestTag != null ? requestTag.hashCode() : 0);
        result = 31 * result + (observe != null ? observe.hashCode() : 0);
        result = 31 * result + (block1Req != null ? block1Req.hashCode() : 0);
        result = 31 * result + (block2Res != null ? block2Res.hashCode() : 0);
        result = 31 * result + (size2Res != null ? size2Res.hashCode() : 0);
        return result;
    }
}
