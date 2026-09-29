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
package opencoap.endpoint;

import static java.util.Objects.requireNonNull;
import static opencoap.util.Validations.require;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import opencoap.core.BlockSize;

/**
 * Message size, block-wise and queueing settings shared by CoAP over UDP and CoAP over TCP servers.
 * <p>
 * Immutable, every {@code with*} method returns a modified copy.
 */
public final class Messaging {
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 1152;
    private static final Messaging DEFAULTS = new Messaging(null, null, 10_000_000, 100, Collections.emptySet());

    private final BlockSize blockSize;
    private final Integer maxMessageSize;
    private final int maxIncomingBlockTransferSize;
    private final int queueMaxSize;
    private final Collection<Integer> recognizedCustomOptions;

    private Messaging(BlockSize blockSize, Integer maxMessageSize, int maxIncomingBlockTransferSize, int queueMaxSize,
            Collection<Integer> recognizedCustomOptions) {
        this.blockSize = blockSize;
        this.maxMessageSize = maxMessageSize;
        this.maxIncomingBlockTransferSize = maxIncomingBlockTransferSize;
        this.queueMaxSize = queueMaxSize;
        this.recognizedCustomOptions = recognizedCustomOptions;
    }

    public static Messaging defaults() {
        return DEFAULTS;
    }

    /**
     * Enables block-wise transfer.
     * <p>
     * On UDP, sets the block size and derives the maximum message size from it, so it can not be combined with
     * {@link #withMaxMessageSize(int)}. BERT block size is not allowed.
     * <p>
     * On TCP, any non-null value enables block-wise transfer; the block size is derived from the maximum message size
     * negotiated with the peer in CSM (RFC 8323).
     *
     * @param blockSize block size, or null to disable block-wise transfer
     * @return modified copy
     */
    public Messaging withBlockSize(BlockSize blockSize) {
        return new Messaging(blockSize, maxMessageSize, maxIncomingBlockTransferSize, queueMaxSize, recognizedCustomOptions);
    }

    /**
     * Sets the maximum message size (default 1152). On TCP, it is advertised to the peer in CSM.
     *
     * @param maxMessageSize maximum message size in bytes
     * @return modified copy
     */
    public Messaging withMaxMessageSize(int maxMessageSize) {
        require(maxMessageSize > 0, "maxMessageSize must be positive");
        return new Messaging(blockSize, maxMessageSize, maxIncomingBlockTransferSize, queueMaxSize, recognizedCustomOptions);
    }

    /**
     * Sets the maximum size of an entity assembled from incoming blocks (default 10 MB).
     *
     * @param maxIncomingBlockTransferSize maximum size in bytes
     * @return modified copy
     */
    public Messaging withMaxIncomingBlockTransferSize(int maxIncomingBlockTransferSize) {
        require(maxIncomingBlockTransferSize > 0, "maxIncomingBlockTransferSize must be positive");
        return new Messaging(blockSize, maxMessageSize, maxIncomingBlockTransferSize, queueMaxSize, recognizedCustomOptions);
    }

    /**
     * Sets the maximum number of outbound requests queued per peer (default 100).
     *
     * @param queueMaxSize maximum queue size
     * @return modified copy
     */
    public Messaging withQueueMaxSize(int queueMaxSize) {
        require(queueMaxSize > 0, "queueMaxSize must be positive");
        return new Messaging(blockSize, maxMessageSize, maxIncomingBlockTransferSize, queueMaxSize, recognizedCustomOptions);
    }

    /**
     * Sets the collection of recognized custom critical CoAP option numbers.
     *
     * @param recognizedCustomOptions a collection of integer option numbers to be recognized as custom options
     * @return modified copy
     */
    public Messaging withRecognizedCustomOptions(Collection<Integer> recognizedCustomOptions) {
        Collection<Integer> options = Collections.unmodifiableSet(new LinkedHashSet<>(requireNonNull(recognizedCustomOptions)));
        return new Messaging(blockSize, maxMessageSize, maxIncomingBlockTransferSize, queueMaxSize, options);
    }

    public BlockSize getBlockSize() {
        return blockSize;
    }

    public int getMaxMessageSize() {
        return maxMessageSize != null ? maxMessageSize : DEFAULT_MAX_MESSAGE_SIZE;
    }

    boolean isMaxMessageSizeSet() {
        return maxMessageSize != null;
    }

    public int getMaxIncomingBlockTransferSize() {
        return maxIncomingBlockTransferSize;
    }

    public int getQueueMaxSize() {
        return queueMaxSize;
    }

    public Collection<Integer> getRecognizedCustomOptions() {
        return recognizedCustomOptions;
    }
}
