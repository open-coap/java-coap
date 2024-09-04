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
package com.mbed.coap.server;

import static com.mbed.coap.transport.TransportContext.RESPONSE_TIMEOUT;
import static com.mbed.coap.utils.Timer.toTimer;
import static com.mbed.coap.utils.Validations.require;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.block.BlockWiseIncomingFilter;
import com.mbed.coap.server.block.BlockWiseNotificationFilter;
import com.mbed.coap.server.block.BlockWiseOutgoingFilter;
import com.mbed.coap.server.filter.CongestionControlFilter;
import com.mbed.coap.server.filter.EchoFilter;
import com.mbed.coap.server.filter.ResponseTimeoutFilter;
import com.mbed.coap.server.messaging.Capabilities;
import com.mbed.coap.server.messaging.CapabilitiesResolver;
import com.mbed.coap.server.messaging.CoapDispatcher;
import com.mbed.coap.server.messaging.CoapRequestConverter;
import com.mbed.coap.server.messaging.DuplicateDetector;
import com.mbed.coap.server.messaging.ExchangeFilter;
import com.mbed.coap.server.messaging.MessageIdSupplier;
import com.mbed.coap.server.messaging.MessageIdSupplierImpl;
import com.mbed.coap.server.messaging.ObservationMapper;
import com.mbed.coap.server.messaging.PiggybackedExchangeFilter;
import com.mbed.coap.server.messaging.RequestTagSupplier;
import com.mbed.coap.server.messaging.RetransmissionFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.transmission.RetransmissionBackOff;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.LoggingCoapTransport;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import com.mbed.coap.utils.Timer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class CoapServerBuilder {
    private static final long DELAYED_TRANSACTION_TIMEOUT_MS = 120000; //2 minutes

    private Supplier<CoapTransport> coapTransport;
    private int duplicationMaxSize = 10000;
    private PutOnlyMap<CoapRequestId, CoapPacket> duplicateDetectionCache;
    private ScheduledExecutorService scheduledExecutorService;
    private MessageIdSupplier midSupplier = new MessageIdSupplierImpl();
    private Duration responseTimeout = Duration.ofMillis(DELAYED_TRANSACTION_TIMEOUT_MS);
    private DuplicatedCoapMessageCallback duplicatedCoapMessageCallback = DuplicatedCoapMessageCallback.NULL;
    private RetransmissionBackOff retransmissionBackOff = RetransmissionBackOff.ofDefault();
    private int maxIncomingBlockTransferSize = 10_000_000; //default to 10 MB
    private BlockSize blockSize;
    private int maxMessageSize = 1152; //default
    private Service<CoapRequest, CoapResponse> route = RouterService.NOT_FOUND_SERVICE;
    private int maxQueueSize = 100;
    private Filter.SimpleFilter<CoapRequest, CoapResponse> outboundFilter = Filter.identity();
    private Filter.SimpleFilter<CoapRequest, CoapResponse> routeFilter = Filter.identity();
    private Filter.SimpleFilter<CoapRequest, CoapResponse> inboundRequestFilter = Filter.identity();
    private NotificationsReceiver notificationsReceiver = NotificationsReceiver.REJECT_ALL;
    private ObservationsStore observationStore = ObservationsStore.ALWAYS_EMPTY;
    private RequestTagSupplier requestTagSupplier = RequestTagSupplier.createSequential();
    private boolean isTransportLoggingEnabled = true;

    CoapServerBuilder() {
    }

    public CoapServerBuilder blockSize(BlockSize blockSize) {
        require(blockSize == null || !blockSize.isBert(), "BlockSize with BERT support is defined only for CoAP over TCP");

        this.blockSize = blockSize;
        return this;
    }

    public CoapServerBuilder transport(CoapTransport coapTransport) {
        requireNonNull(coapTransport);
        return transport(() -> coapTransport);
    }

    public CoapServerBuilder transport(Supplier<CoapTransport> coapTransport) {
        this.coapTransport = requireNonNull(coapTransport);
        return this;
    }

    public CoapServerBuilder route(Service<CoapRequest, CoapResponse> route) {
        this.route = requireNonNull(route);
        return this;
    }

    public CoapServerBuilder route(RouterService.RouteBuilder routeBuilder) {
        return route(routeBuilder.build());
    }

    public CoapServerBuilder routeFilter(Filter.SimpleFilter<CoapRequest, CoapResponse> routeFilter) {
        this.routeFilter = requireNonNull(routeFilter);
        return this;
    }

    public CoapServerBuilder inboundRequestFilter(Filter.SimpleFilter<CoapRequest, CoapResponse> inboundRequestFilter) {
        this.inboundRequestFilter = requireNonNull(inboundRequestFilter);
        return this;
    }

    public CoapServerBuilder outboundFilter(Filter.SimpleFilter<CoapRequest, CoapResponse> outboundFilter) {
        this.outboundFilter = requireNonNull(outboundFilter);
        return this;
    }

    public CoapServerBuilder notificationsReceiver(NotificationsReceiver notificationsReceiver) {
        this.notificationsReceiver = requireNonNull(notificationsReceiver);
        if (observationStore.equals(ObservationsStore.ALWAYS_EMPTY)) {
            return observationsStore(ObservationsStore.inMemory());
        }
        return this;
    }

    public CoapServerBuilder observationsStore(ObservationsStore observationsStore) {
        this.observationStore = requireNonNull(observationsStore);
        return this;
    }

    public CoapServerBuilder maxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        return this;
    }

    public CoapServerBuilder maxIncomingBlockTransferSize(int size) {
        this.maxIncomingBlockTransferSize = size;
        return this;
    }

    private PutOnlyMap<CoapRequestId, CoapPacket> getOrCreateDuplicateDetectorCache(ScheduledExecutorService scheduledExecutorService) {
        if (duplicateDetectionCache != null) {
            return duplicateDetectionCache;
        }
        return new DefaultDuplicateDetectorCache("Default cache", duplicationMaxSize, scheduledExecutorService);
    }

    private CapabilitiesResolver capabilities() {
        Capabilities defaultCapability;
        if (blockSize != null) {
            defaultCapability = new Capabilities(blockSize.getSize() + 1, true, requestTagSupplier);
        } else {
            defaultCapability = new Capabilities(maxMessageSize, false, requestTagSupplier);
        }

        return __ -> defaultCapability;
    }

    public CoapServerBuilder executor(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    public CoapServerBuilder midSupplier(MessageIdSupplier midSupplier) {
        this.midSupplier = midSupplier;
        return this;
    }

    public CoapServerBuilder retransmission(RetransmissionBackOff retransmissionBackOff) {
        this.retransmissionBackOff = retransmissionBackOff;
        return this;
    }

    public CoapServerBuilder responseTimeout(Duration timeout) {
        require(timeout.toMillis() > 0);
        this.responseTimeout = timeout;
        return this;
    }

    public CoapServerBuilder duplicatedCoapMessageCallback(DuplicatedCoapMessageCallback duplicatedCallback) {
        this.duplicatedCoapMessageCallback = requireNonNull(duplicatedCallback);
        return this;
    }

    public CoapServerBuilder queueMaxSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public CoapServerBuilder duplicateMsgCacheSize(int duplicationMaxSize) {
        require(duplicationMaxSize > 0);
        this.duplicationMaxSize = duplicationMaxSize;
        return this;
    }

    public CoapServerBuilder duplicateMessageDetectorCache(PutOnlyMap<CoapRequestId, CoapPacket> duplicateDetectionCache) {
        this.duplicateDetectionCache = duplicateDetectionCache;
        return this;
    }

    public CoapServerBuilder noDuplicateCheck() {
        this.duplicationMaxSize = -1;
        return this;
    }

    public CoapServerBuilder requestTagSupplier(RequestTagSupplier requestTagSupplier) {
        this.requestTagSupplier = requireNonNull(requestTagSupplier);
        return this;
    }

    public CoapServerBuilder transportLogging(boolean isTransportLoggingEnabled) {
        this.isTransportLoggingEnabled = isTransportLoggingEnabled;
        return this;
    }

    public CoapServer build() {
        CoapTransport realTransport = requireNonNull(this.coapTransport.get(), "Missing transport");
        CoapTransport coapTransport = isTransportLoggingEnabled ? new LoggingCoapTransport(realTransport) : realTransport;
        final boolean stopExecutor = scheduledExecutorService == null;
        final ScheduledExecutorService effectiveExecutorService = scheduledExecutorService != null ? scheduledExecutorService : Executors.newSingleThreadScheduledExecutor();
        Timer timer = toTimer(effectiveExecutorService);

        Service<CoapPacket, Boolean> sender = coapTransport::sendPacket;

        // OUTBOUND
        ExchangeFilter exchangeFilter = new ExchangeFilter();
        RetransmissionFilter<CoapPacket, CoapPacket> retransmissionFilter = new RetransmissionFilter<>(timer, retransmissionBackOff, CoapPacket::isConfirmable);
        PiggybackedExchangeFilter piggybackedExchangeFilter = new PiggybackedExchangeFilter();

        Service<CoapRequest, CoapResponse> outboundService = outboundFilter
                .andThen(new ObserveRequestFilter(observationStore::add))
                .andThen(new CongestionControlFilter<>(maxQueueSize, CoapRequest::getPeerAddress))
                .andThen(new BlockWiseOutgoingFilter(capabilities(), maxIncomingBlockTransferSize))
                .andThen(new EchoFilter())
                .andThen(new ResponseTimeoutFilter<>(timer, req -> req.getTransContext(RESPONSE_TIMEOUT, responseTimeout)))
                .andThen(exchangeFilter)
                .andThen(Filter.of(CoapPacket::from, CoapPacket::toCoapResponse)) // convert coap packet
                .andThenMap(midSupplier::update)
                .andThen(retransmissionFilter)
                .andThen(piggybackedExchangeFilter)
                .then(sender);


        // OBSERVATION
        Service<SeparateResponse, Boolean> sendNotification = new NotificationValidator()
                .andThen(new BlockWiseNotificationFilter(capabilities()))
                .andThen(new ResponseTimeoutFilter<>(timer, req -> req.getTransContext(RESPONSE_TIMEOUT, responseTimeout)))
                .andThen(Filter.of(CoapPacket::from, CoapPacket::isAck))
                .andThenMap(midSupplier::update)
                .andThen(retransmissionFilter)
                .andThen(piggybackedExchangeFilter)
                .then(sender);

        // INBOUND
        PutOnlyMap<CoapRequestId, CoapPacket> duplicateDetectorCache = getOrCreateDuplicateDetectorCache(effectiveExecutorService);
        DuplicateDetector duplicateDetector = new DuplicateDetector(duplicateDetectorCache, duplicatedCoapMessageCallback);
        Service<CoapPacket, CoapPacket> inboundService = duplicateDetector
                .andThen(new CoapRequestConverter(midSupplier))
                .andThen(inboundRequestFilter)
                .andThen(new RescueFilter())
                .andThen(new CriticalOptionVerifier())
                .andThen(new BlockWiseIncomingFilter(capabilities(), maxIncomingBlockTransferSize))
                .andThen(routeFilter)
                .then(route);


        Service<CoapPacket, CoapPacket> inboundObservation = duplicateDetector
                .andThen(new ObservationMapper())
                .then(new ObservationHandler(notificationsReceiver, observationStore));

        CoapDispatcher dispatcher = new CoapDispatcher(sender, inboundObservation, inboundService,
                piggybackedExchangeFilter::handleResponse, exchangeFilter::handleResponse
        );

        return new CoapServer(coapTransport, dispatcher::handle, outboundService, sendNotification, () -> {
            piggybackedExchangeFilter.stop();
            duplicateDetectorCache.stop();
            if (stopExecutor) {
                effectiveExecutorService.shutdown();
            }
        });

    }

    public CoapClient buildClient(InetSocketAddress target) throws IOException {
        return CoapClient.create(target, build().start());
    }

    public CoapServerGroup buildGroup(int size) {
        List<CoapServer> servers = Stream.generate(this::build)
                .limit(size)
                .collect(toList());

        return new CoapServerGroup(servers);
    }
}
