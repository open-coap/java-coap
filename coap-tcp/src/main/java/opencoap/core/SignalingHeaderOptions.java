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

import static opencoap.util.Validations.require;
import java.util.List;
import java.util.Objects;
import opencoap.codec.RawOption;

/**
 * Implements CoAP additional header options from
 * - RFC 8323
 */
public class SignalingHeaderOptions extends HeaderOptions {

    private static final byte SIGN_OPTION_2 = 2;
    private static final byte SIGN_OPTION_4 = 4;
    private final Code code;
    private Opaque signalingOption2;
    private Opaque signalingOption4;

    public SignalingHeaderOptions(Code code) {
        require(code.isSignaling());
        this.code = code;
    }

    @Override
    public boolean parseOption(int type, Opaque data) {
        switch (type) {
            case SIGN_OPTION_2:
                signalingOption2 = data;
                break;
            case SIGN_OPTION_4:
                signalingOption4 = data;
                break;
            default:
                return super.parseOption(type, data);

        }
        return true;
    }

    @Override
    public List<RawOption> getRawOptions() {
        List<RawOption> l = super.getRawOptions();
        if (signalingOption2 != null) {
            l.add(new RawOption(SIGN_OPTION_2, signalingOption2));
        }
        if (signalingOption4 != null) {
            l.add(new RawOption(SIGN_OPTION_4, signalingOption4));
        }

        return l;
    }

    @Override
    public void buildToString(StringBuilder sb) {
        super.buildToString(sb);

        if (signalingOption2 != null || signalingOption4 != null) {
            SignalingOptions signOpt = new SignalingOptions();
            if (signalingOption2 != null) {
                signOpt.parse(2, signalingOption2, code);
            }
            if (signalingOption4 != null) {
                signOpt.parse(4, signalingOption4, code);
            }
            sb.append(signOpt.toString());
        }
    }

    public SignalingOptions toSignalingOptions(Code code) {
        if (signalingOption2 == null && signalingOption4 == null) {
            return null;
        } else {
            SignalingOptions signalingOptions = new SignalingOptions();
            if (signalingOption2 != null) {
                signalingOptions.parse(SIGN_OPTION_2, signalingOption2, code);
            }
            if (signalingOption4 != null) {
                signalingOptions.parse(SIGN_OPTION_4, signalingOption4, code);
            }
            return signalingOptions;
        }
    }

    public void putSignalingOptions(SignalingOptions signalingOptions) {
        this.signalingOption2 = signalingOptions.serializeOption2();
        this.signalingOption4 = signalingOptions.serializeOption4();
    }

    @Override
    public HeaderOptions duplicate() {
        SignalingHeaderOptions opts = new SignalingHeaderOptions(code);
        super.duplicate(opts);

        opts.signalingOption2 = signalingOption2;
        opts.signalingOption4 = signalingOption4;

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

        SignalingHeaderOptions that = (SignalingHeaderOptions) o;
        return code == that.code && Objects.equals(signalingOption2, that.signalingOption2) && Objects.equals(signalingOption4, that.signalingOption4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code, signalingOption2, signalingOption4);
    }
}
