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
package opencoap.endpoint;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static opencoap.core.MessageAttributes.RESPONSE_TIMEOUT;
import static opencoap.util.Scheduler.toScheduler;
import static opencoap.util.Validations.require;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import opencoap.codec.CoapPacket;
import opencoap.core.BlockSize;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Filter;
import opencoap.core.MappingFilter;
import opencoap.core.SeparateResponse;
import opencoap.core.Handler;
import opencoap.endpoint.pipeline.BlockWiseIncomingFilter;
import opencoap.endpoint.pipeline.BlockWiseNotificationFilter;
import opencoap.endpoint.pipeline.BlockWiseOutgoingFilter;
import opencoap.endpoint.pipeline.CoapRequestConverter;
import opencoap.endpoint.pipeline.CriticalOptionVerifier;
import opencoap.endpoint.pipeline.DuplicateDetector;
import opencoap.endpoint.pipeline.ExchangeFilter;
import opencoap.endpoint.pipeline.PiggybackedExchangeFilter;
import opencoap.endpoint.pipeline.RescueFilter;
import opencoap.endpoint.pipeline.RetransmissionFilter;
import opencoap.filter.CongestionControlFilter;
import opencoap.filter.EchoFilter;
import opencoap.filter.ResponseTimeoutFilter;
import opencoap.observe.NotificationValidator;
import opencoap.observe.ObservationHandler;
import opencoap.observe.ObservationMapper;
import opencoap.observe.ObservationsStore;
import opencoap.observe.ObserveRequestFilter;
import opencoap.routing.RoutingHandler;
import opencoap.transport.CoapTransport;
import opencoap.transport.LoggingCoapTransport;
import opencoap.util.Scheduler;

@SuppressWarnings("PMD.CouplingBetweenObjects") // it's a nature for a builder class to have many dependencies
public final class CoapServerBuilder {
    private Supplier<CoapTransport> coapTransport;
    private ScheduledExecutorService scheduledExecutorService;
    private Handler<CoapRequest, CoapResponse> handler = RoutingHandler.NOT_FOUND;
    private Messaging messaging = Messaging.defaults();
    private Reliability reliability = Reliability.defaults();
    private Observations observations = Observations.none();
    private Filter<CoapRequest, CoapResponse> outboundFilter = Filter.identity();
    private Filter<CoapRequest, CoapResponse> routeFilter = Filter.identity();
    private Filter<CoapRequest, CoapResponse> inboundFilter = Filter.identity();
    private boolean isTransportLoggingEnabled = true;

    CoapServerBuilder() {
    }

    public CoapServerBuilder transport(CoapTransport coapTransport) {
        requireNonNull(coapTransport);
        return transport(() -> coapTransport);
    }

    public CoapServerBuilder transport(Supplier<CoapTransport> coapTransport) {
        this.coapTransport = requireNonNull(coapTransport);
        return this;
    }

    public CoapServerBuilder handler(Handler<CoapRequest, CoapResponse> handler) {
        this.handler = requireNonNull(handler);
        return this;
    }

    public CoapServerBuilder handler(RoutingHandler.RouteBuilder routeBuilder) {
        return handler(routeBuilder.build());
    }

    /**
     * Sets message size, block-wise and queueing settings.
     *
     * @param messaging messaging settings
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if both block size and max message size are set, or block size is BERT
     */
    public CoapServerBuilder messaging(Messaging messaging) {
        requireNonNull(messaging);
        BlockSize blockSize = messaging.getBlockSize();
        require(blockSize == null || !blockSize.isBert(), "BlockSize with BERT support is defined only for CoAP over TCP");
        require(blockSize == null || !messaging.isMaxMessageSizeSet(), "On CoAP over UDP, maxMessageSize is derived from blockSize, set only one of them");

        this.messaging = messaging;
        return this;
    }

    /**
     * Modifies current message size, block-wise and queueing settings.
     *
     * @param modifier function that receives current settings and returns modified ones
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if both block size and max message size are set, or block size is BERT
     */
    public CoapServerBuilder messaging(UnaryOperator<Messaging> modifier) {
        return messaging(modifier.apply(messaging));
    }

    public CoapServerBuilder reliability(Reliability reliability) {
        this.reliability = requireNonNull(reliability);
        return this;
    }

    /**
     * Modifies current retransmission, response timeout, message id, request tag and duplicate detection settings.
     *
     * @param modifier function that receives current settings and returns modified ones
     * @return this builder instance for method chaining
     */
    public CoapServerBuilder reliability(UnaryOperator<Reliability> modifier) {
        return reliability(modifier.apply(reliability));
    }

    public CoapServerBuilder observations(Observations observations) {
        this.observations = requireNonNull(observations);
        return this;
    }

    public CoapServerBuilder executor(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    public CoapServerBuilder transportLogging(boolean isTransportLoggingEnabled) {
        this.isTransportLoggingEnabled = isTransportLoggingEnabled;
        return this;
    }

    public CoapServerBuilder inboundFilter(Filter<CoapRequest, CoapResponse> inboundFilter) {
        this.inboundFilter = requireNonNull(inboundFilter);
        return this;
    }

    public CoapServerBuilder routeFilter(Filter<CoapRequest, CoapResponse> routeFilter) {
        this.routeFilter = requireNonNull(routeFilter);
        return this;
    }

    public CoapServerBuilder outboundFilter(Filter<CoapRequest, CoapResponse> outboundFilter) {
        this.outboundFilter = requireNonNull(outboundFilter);
        return this;
    }

    private CapabilitiesResolver capabilities(RequestTagSupplier requestTagSupplier) {
        BlockSize blockSize = messaging.getBlockSize();
        Capabilities defaultCapability;
        if (blockSize != null) {
            defaultCapability = new Capabilities(blockSize.getSize() + 1, true, requestTagSupplier);
        } else {
            defaultCapability = new Capabilities(messaging.getMaxMessageSize(), false, requestTagSupplier);
        }

        return __ -> defaultCapability;
    }

    public CoapServer build() {
        CoapTransport realTransport = requireNonNull(this.coapTransport.get(), "Missing transport");
        CoapTransport coapTransport = isTransportLoggingEnabled ? LoggingCoapTransport.wrap(realTransport) : realTransport;
        final boolean stopExecutor = scheduledExecutorService == null;
        final ScheduledExecutorService effectiveExecutorService = scheduledExecutorService != null ? scheduledExecutorService : Executors.newSingleThreadScheduledExecutor();
        Scheduler scheduler = toScheduler(effectiveExecutorService);
        MessageIdSupplier messageIdSupplier = reliability.resolveMessageIdSupplier();
        CapabilitiesResolver capabilities = capabilities(reliability.resolveRequestTagSupplier());
        Duration responseTimeout = reliability.getResponseTimeout();
        ObservationsStore observationStore = observations.createStore();

        Handler<CoapPacket, Boolean> sender = coapTransport::sendPacket;

        // OUTBOUND
        ExchangeFilter exchangeFilter = new ExchangeFilter();
        RetransmissionFilter<CoapPacket, CoapPacket> retransmissionFilter = new RetransmissionFilter<>(scheduler, reliability.getRetransmission(), CoapPacket::isConfirmable);
        PiggybackedExchangeFilter piggybackedExchangeFilter = new PiggybackedExchangeFilter();

        Handler<CoapRequest, CoapResponse> outboundHandler = outboundFilter
                .andThen(new ObserveRequestFilter(observationStore::add))
                .andThen(new CongestionControlFilter<>(messaging.getQueueMaxSize(), CoapRequest::getPeerAddress))
                .andThen(new BlockWiseOutgoingFilter(capabilities, messaging.getMaxIncomingBlockTransferSize()))
                .andThen(new EchoFilter())
                .andThen(new ResponseTimeoutFilter<>(scheduler, req -> req.getAttribute(RESPONSE_TIMEOUT, responseTimeout)))
                .andThen(exchangeFilter)
                .andThen(MappingFilter.of(CoapPacket::from, CoapPacket::toCoapResponse)) // convert coap packet
                .andThenMap(messageIdSupplier::update)
                .andThen(retransmissionFilter)
                .andThen(piggybackedExchangeFilter)
                .then(sender);


        // OBSERVATION
        Handler<SeparateResponse, Boolean> sendNotification = new NotificationValidator()
                .andThen(new BlockWiseNotificationFilter(capabilities))
                .andThen(new ResponseTimeoutFilter<>(scheduler, req -> req.getAttribute(RESPONSE_TIMEOUT, responseTimeout)))
                .andThen(MappingFilter.of(CoapPacket::from, CoapPacket::isAck))
                .andThenMap(messageIdSupplier::update)
                .andThen(retransmissionFilter)
                .andThen(piggybackedExchangeFilter)
                .then(sender);

        // INBOUND
        DuplicateDetection duplicateDetection = reliability.getDuplicateDetection();
        PutOnlyMap<CoapMessageKey, CoapPacket> duplicateDetectorCache = duplicateDetection.isEnabled()
                ? duplicateDetection.createCache(effectiveExecutorService)
                : null;
        Filter<CoapPacket, CoapPacket> duplicateDetector = duplicateDetectorCache != null
                ? new DuplicateDetector(duplicateDetectorCache, duplicateDetection.getCallback())
                : Filter.identity();
        Handler<CoapPacket, CoapPacket> inboundService = duplicateDetector
                .andThen(new CoapRequestConverter(messageIdSupplier))
                .andThen(inboundFilter)
                .andThen(new RescueFilter())
                .andThen(new CriticalOptionVerifier(messaging.getRecognizedCustomOptions()))
                .andThen(new BlockWiseIncomingFilter(capabilities, messaging.getMaxIncomingBlockTransferSize()))
                .andThen(routeFilter)
                .then(handler);


        Handler<CoapPacket, CoapPacket> inboundObservation = duplicateDetector
                .andThen(new ObservationMapper())
                .then(new ObservationHandler(observations.getReceiver(), observationStore));

        CoapDispatcher dispatcher = new CoapDispatcher(sender, inboundObservation, inboundService,
                piggybackedExchangeFilter::handleResponse, exchangeFilter::handleResponse
        );

        return new CoapServer(coapTransport, dispatcher::handle, outboundHandler, sendNotification, () -> {
            piggybackedExchangeFilter.stop();
            if (duplicateDetectorCache != null) {
                duplicateDetectorCache.stop();
            }
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
