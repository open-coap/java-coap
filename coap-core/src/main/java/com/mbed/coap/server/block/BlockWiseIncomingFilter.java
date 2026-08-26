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
package com.mbed.coap.server.block;

import static com.mbed.coap.packet.CoapResponse.coapResponse;
import static com.mbed.coap.utils.FutureHelpers.failedFuture;
import static com.mbed.coap.utils.Validations.require;
import com.mbed.coap.CoapConstants;
import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.BlockOption;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.messaging.CapabilitiesResolver;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import com.mbed.coap.utils.Timer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockWiseIncomingFilter implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockWiseIncomingFilter.class.getName());

    /**
     * Together with maximum transfer size it gives the worst case memory that incoming block-wise transfers can hold.
     */
    public static final int DEFAULT_MAX_INCOMING_BLOCK_TRANSFERS = 100;

    /**
     * A transfer that received no block for that long is dropped. EXCHANGE_LIFETIME is the point after which a peer
     * can no longer expect a reply to the block it sent last, so a transfer idle for longer can not be resumed anyway.
     */
    public static final Duration DEFAULT_IDLE_TIMEOUT = CoapConstants.EXCHANGE_LIFETIME;

    private static final int SWEEPS_PER_IDLE_TIMEOUT = 4;
    private static final long WARN_INTERVAL_MILLIS = 10_000;

    private final Map<BlockRequestId, BlockWiseIncomingTransaction> blockReqMap = new ConcurrentHashMap<>();
    private final CapabilitiesResolver capabilities;
    private final int maxIncomingBlockTransferSize;
    private final int maxIncomingBlockTransfers;
    private final long idleTimeoutMillis;
    private final Duration sweepInterval;
    private final Timer timer;
    private final LongSupplier clock;
    private final AtomicReference<Runnable> cancelSweep = new AtomicReference<>(() -> {
    });
    private final AtomicBoolean isSweepScheduled = new AtomicBoolean();
    private volatile boolean isStopped;
    private long nextWarnTimestamp;

    /**
     * Creates a filter that only expires idle transfers when a new one arrives. Use the constructor with a timer to
     * also release them while the server is idle.
     *
     * @param capabilities peer capabilities
     * @param maxIncomingBlockTransferSize maximum size of a single assembled transfer
     */
    public BlockWiseIncomingFilter(CapabilitiesResolver capabilities, int maxIncomingBlockTransferSize) {
        this(capabilities, maxIncomingBlockTransferSize, DEFAULT_MAX_INCOMING_BLOCK_TRANSFERS, DEFAULT_IDLE_TIMEOUT, Timer.NOOP);
    }

    /**
     * @param capabilities peer capabilities
     * @param maxIncomingBlockTransferSize maximum size of a single assembled transfer
     * @param maxIncomingBlockTransfers maximum number of transfers kept in flight at the same time
     * @param idleTimeout how long a transfer may receive no block before it is dropped
     * @param timer timer that runs the background sweeper
     */
    public BlockWiseIncomingFilter(CapabilitiesResolver capabilities, int maxIncomingBlockTransferSize, int maxIncomingBlockTransfers,
            Duration idleTimeout, Timer timer) {
        this(capabilities, maxIncomingBlockTransferSize, maxIncomingBlockTransfers, idleTimeout, timer, System::currentTimeMillis);
    }

    BlockWiseIncomingFilter(CapabilitiesResolver capabilities, int maxIncomingBlockTransferSize, int maxIncomingBlockTransfers,
            Duration idleTimeout, Timer timer, LongSupplier clock) {
        require(maxIncomingBlockTransfers > 0, "Maximum number of block-wise transfers must be positive");
        require(idleTimeout.toMillis() > 0, "Block-wise transfer idle timeout must be positive");

        this.capabilities = capabilities;
        this.maxIncomingBlockTransferSize = maxIncomingBlockTransferSize;
        this.maxIncomingBlockTransfers = maxIncomingBlockTransfers;
        this.idleTimeoutMillis = idleTimeout.toMillis();
        this.sweepInterval = maxDuration(idleTimeout.dividedBy(SWEEPS_PER_IDLE_TIMEOUT), Duration.ofMillis(1));
        this.timer = timer;
        this.clock = clock;
    }

    private static Duration maxDuration(Duration first, Duration second) {
        return first.compareTo(second) < 0 ? second : first;
    }

    /**
     * Stops the background sweeper. Transfers that are still in flight are dropped.
     */
    public void stop() {
        isStopped = true;
        cancelSweep.getAndSet(() -> {
        }).run();
        blockReqMap.clear();
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
        BlockOption reqBlock = request.options().getBlock1Req();

        if (reqBlock == null) {
            final CoapRequest coapRequest = request;
            return service.apply(request)
                    .thenApply(resp -> adjustPayloadSize(coapRequest, resp));
        }

        //block wise transaction
        BlockRequestId blockRequestId = BlockRequestId.from(request);
        BlockWiseIncomingTransaction blockRequest = (reqBlock.getNr() == 0) ? null : blockReqMap.get(blockRequestId);

        try {
            if (blockRequest == null && reqBlock.getNr() != 0) {
                //Could not find previous blocks
                LOGGER.warn("Could not find previous blocks for {}", request);
                throw new CoapCodeException(Code.C408_REQUEST_ENTITY_INCOMPLETE, "no prev blocks");
            } else if (blockRequest != null && !blockRequest.validateRequestTag(request)) {
                LOGGER.warn("[{}] Mismatch request-tag: {}", request.getPeerAddress(), request.options().getRequestTag());
                throw new CoapCodeException(Code.C408_REQUEST_ENTITY_INCOMPLETE, "Mismatch request-tag");
            } else if (blockRequest == null) {
                //start new block-wise transaction, reject a declared size before allocating for it
                BlockWiseIncomingTransaction.validateSize1(request, maxIncomingBlockTransferSize);
                blockRequest = new BlockWiseIncomingTransaction(request, maxIncomingBlockTransferSize, capabilities.getOrDefault(request.getPeerAddress()));
                putWithinBounds(blockRequestId, blockRequest);
            }
            blockRequest.appendBlock(request);
            blockRequest.updateLastActivity(clock.getAsLong());

        } catch (CoapCodeException e) {
            removeBlockRequest(blockRequestId);
            return failedFuture(e);
        }

        if (!reqBlock.hasMore()) {
            //remove from map
            removeBlockRequest(blockRequestId);

            //last block received
            final CoapRequest coapRequest = request.modify().payload(blockRequest.getCombinedPayload()).build();
            return service
                    .apply(coapRequest)
                    .thenApply(resp -> adjustPayloadSize(coapRequest, resp));
        } else {
            //more block available, send C231_CONTINUE
            BlockSize localBlockSize = agreedBlockSize(request.getPeerAddress());

            if (localBlockSize != null && reqBlock.getSize() > localBlockSize.getSize()) {
                //to large block, change
                LOGGER.trace("to large block (" + reqBlock.getSize() + "), changing to " + localBlockSize.getSize());
                reqBlock = new BlockOption(reqBlock.getNr(), localBlockSize, reqBlock.hasMore());
            }

            final BlockOption finalReqBlock = reqBlock;
            return coapResponse(Code.C231_CONTINUE)
                    .options(o -> o.block1Req(finalReqBlock))
                    .toFuture();
        }
    }

    private BlockSize agreedBlockSize(InetSocketAddress address) {
        return capabilities.getOrDefault(address).getBlockSize();
    }

    private void removeBlockRequest(BlockRequestId blockRequestId) {
        blockReqMap.remove(blockRequestId);
    }

    private void putWithinBounds(BlockRequestId blockRequestId, BlockWiseIncomingTransaction blockRequest) {
        long now = clock.getAsLong();
        blockRequest.updateLastActivity(now);

        synchronized (blockReqMap) {
            dropIdleTransfers(now);

            // an abandoned transfer is idle, so it is the first to go when there is no room left
            for (int size = blockReqMap.size(); size >= maxIncomingBlockTransfers; size--) {
                dropLeastRecentlyActiveTransfer();
            }
            blockReqMap.put(blockRequestId, blockRequest);
        }
        startSweeping();
    }

    private void sweep() {
        dropIdleTransfers(clock.getAsLong());

        if (!blockReqMap.isEmpty()) {
            scheduleSweep();
            return;
        }

        //nothing left to expire, the next transfer starts the sweeper again
        isSweepScheduled.set(false);
        if (!blockReqMap.isEmpty()) {
            startSweeping();
        }
    }

    private void startSweeping() {
        if (isSweepScheduled.compareAndSet(false, true)) {
            scheduleSweep();
        }
    }

    private void scheduleSweep() {
        if (isStopped) {
            return;
        }
        cancelSweep.set(timer.schedule(sweepInterval, this::sweep));
    }

    private void dropIdleTransfers(long now) {
        blockReqMap.entrySet().removeIf(entry -> {
            boolean isIdle = now - entry.getValue().getLastActivity() > idleTimeoutMillis;
            if (isIdle) {
                LOGGER.debug("Dropping idle block-wise transfer {}", entry.getKey());
            }
            return isIdle;
        });
    }

    private void dropLeastRecentlyActiveTransfer() {
        BlockRequestId leastRecent = null;
        long leastRecentActivity = Long.MAX_VALUE;

        for (Map.Entry<BlockRequestId, BlockWiseIncomingTransaction> entry : blockReqMap.entrySet()) {
            if (entry.getValue().getLastActivity() < leastRecentActivity) {
                leastRecentActivity = entry.getValue().getLastActivity();
                leastRecent = entry.getKey();
            }
        }

        if (leastRecent != null) {
            blockReqMap.remove(leastRecent);
            warnAboutReachedLimit(leastRecent);
        }
    }

    private void warnAboutReachedLimit(BlockRequestId dropped) {
        long now = clock.getAsLong();
        if (now >= nextWarnTimestamp) {
            LOGGER.warn("Reached maximum number of block-wise transfers ({}), dropped the least recently active one: {}",
                    maxIncomingBlockTransfers, dropped);
            nextWarnTimestamp = now + WARN_INTERVAL_MILLIS;
        }
    }

    public CoapResponse adjustPayloadSize(CoapRequest req, CoapResponse resp) {
        if (resp.getCode().isError()) {
            return resp;
        }

        resp.options().setBlock1Req(req.options().getBlock1Req());
        if (resp.options().getBlock2Res() == null) {

            //check for blocking
            BlockOption block2Res = req.options().getBlock2Res();

            if (block2Res == null && capabilities.getOrDefault(req.getPeerAddress()).useBlockTransfer(resp.getPayload())) {
                block2Res = new BlockOption(0, agreedBlockSize(req.getPeerAddress()), true);
            }

            if (block2Res != null && req.options().getObserve() == null) {
                return updateBlockResponse(block2Res, req, resp);
            }
        }

        return resp;
    }

    private CoapResponse updateBlockResponse(final BlockOption block2Response, final CoapRequest req, final CoapResponse resp) {
        BlockOption block2Res = block2Response;
        int blFrom = block2Res.getNr() * block2Res.getSize();

        int maxMessageSize = !block2Res.isBert() ? block2Res.getSize() : capabilities.getOrDefault(req.getPeerAddress()).getMaxOutboundPayloadSize();

        int blTo = blFrom + maxMessageSize;

        if (blTo + 1 >= resp.getPayload().size()) {
            blTo = resp.getPayload().size();
            block2Res = new BlockOption(block2Res.getNr(), block2Res.getBlockSize(), false);
        } else {
            block2Res = new BlockOption(block2Res.getNr(), block2Res.getBlockSize(), true);
        }

        int newLength = blTo - blFrom;
        if (newLength < 0) {
            newLength = 0;
        }
        // reply with payload size only in first block
        // see https://tools.ietf.org/html/draft-ietf-core-block-18#section-4 , Implementation notes
        if (req.options().getSize2Res() != null && block2Res.getNr() == 0) {
            resp.options().setSize2Res(resp.getPayload().size());
        }
        Opaque blockPayload = resp.getPayload().slice(blFrom, newLength);
        resp.options().setBlock2Res(block2Res);
        return resp.withPayload(blockPayload);
    }

}
