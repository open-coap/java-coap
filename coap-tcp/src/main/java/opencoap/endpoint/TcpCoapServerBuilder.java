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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import opencoap.codec.CoapPacket;
import opencoap.codec.CoapTcpPacketConverter;
import opencoap.core.CoapRequest;
import opencoap.core.CoapResponse;
import opencoap.core.Code;
import opencoap.core.Filter;
import opencoap.core.SeparateResponse;
import opencoap.core.Handler;
import opencoap.endpoint.pipeline.BlockWiseIncomingFilter;
import opencoap.endpoint.pipeline.BlockWiseNotificationFilter;
import opencoap.endpoint.pipeline.BlockWiseOutgoingFilter;
import opencoap.endpoint.pipeline.CriticalOptionVerifier;
import opencoap.endpoint.pipeline.MaxMessageSizeFilter;
import opencoap.endpoint.pipeline.RescueFilter;
import opencoap.endpoint.pipeline.TcpExchangeFilter;
import opencoap.filter.CongestionControlFilter;
import opencoap.observe.NotificationValidator;
import opencoap.observe.ObservationHandler;
import opencoap.observe.ObservationsStore;
import opencoap.observe.ObserveRequestFilter;
import opencoap.routing.RoutingHandler;
import opencoap.transport.CoapTcpTransport;
import opencoap.transport.LoggingCoapTransport;

public class TcpCoapServerBuilder {
    private CoapTcpTransport coapTransport;
    private Handler<CoapRequest, CoapResponse> handler = RoutingHandler.NOT_FOUND;
    private CapabilitiesStorage csmStorage;
    private Messaging messaging = Messaging.defaults();
    private Observations observations = Observations.none();
    private Filter<CoapRequest, CoapResponse> outboundFilter = Filter.identity();
    private Filter<CoapRequest, CoapResponse> routeFilter = Filter.identity();
    private Filter<CoapRequest, CoapResponse> inboundFilter = Filter.identity();
    private boolean isTransportLoggingEnabled = true;

    TcpCoapServerBuilder() {
        csmStorage = new HashMapCapabilitiesStorage();
    }

    private CapabilitiesStorage capabilities() {
        return csmStorage;
    }

    public TcpCoapServerBuilder transport(CoapTcpTransport coapTransport) {
        this.coapTransport = requireNonNull(coapTransport);
        return this;
    }

    public TcpCoapServerBuilder handler(Handler<CoapRequest, CoapResponse> handler) {
        this.handler = requireNonNull(handler);
        return this;
    }

    public TcpCoapServerBuilder handler(RoutingHandler.RouteBuilder routeBuilder) {
        return handler(routeBuilder.build());
    }

    /**
     * Sets message size, block-wise and queueing settings. Any non-null block size enables block-wise transfer, the
     * block size itself is derived from the maximum message size negotiated in CSM.
     *
     * @param messaging messaging settings
     * @return this builder instance for method chaining
     */
    public TcpCoapServerBuilder messaging(Messaging messaging) {
        this.messaging = requireNonNull(messaging);
        return this;
    }

    /**
     * Modifies current message size, block-wise and queueing settings.
     *
     * @param modifier function that receives current settings and returns modified ones
     * @return this builder instance for method chaining
     */
    public TcpCoapServerBuilder messaging(UnaryOperator<Messaging> modifier) {
        return messaging(modifier.apply(messaging));
    }

    public TcpCoapServerBuilder observations(Observations observations) {
        this.observations = requireNonNull(observations);
        return this;
    }

    public TcpCoapServerBuilder csmStorage(CapabilitiesStorage csmStorage) {
        this.csmStorage = requireNonNull(csmStorage);
        return this;
    }

    public TcpCoapServerBuilder transportLogging(boolean isTransportLoggingEnabled) {
        this.isTransportLoggingEnabled = isTransportLoggingEnabled;
        return this;
    }

    public TcpCoapServerBuilder inboundFilter(Filter<CoapRequest, CoapResponse> inboundFilter) {
        this.inboundFilter = requireNonNull(inboundFilter);
        return this;
    }

    public TcpCoapServerBuilder routeFilter(Filter<CoapRequest, CoapResponse> routeFilter) {
        this.routeFilter = requireNonNull(routeFilter);
        return this;
    }

    public TcpCoapServerBuilder outboundFilter(Filter<CoapRequest, CoapResponse> outboundFilter) {
        this.outboundFilter = requireNonNull(outboundFilter);
        return this;
    }

    public CoapClient buildClient(InetSocketAddress target) throws IOException {
        return CoapClient.create(target, build().start(), r -> r.getCode() == Code.C703_PONG);
    }

    public CoapServer build() {
        ObservationsStore observationsStore = observations.createStore();
        Handler<CoapPacket, Boolean> sender = (isTransportLoggingEnabled ? LoggingCoapTransport.wrap(coapTransport) : coapTransport)::sendPacket;

        // NOTIFICATION
        Handler<SeparateResponse, Boolean> sendNotification = new NotificationValidator()
                .andThen(new BlockWiseNotificationFilter(capabilities()))
                .andThenMap(CoapTcpPacketConverter::toCoapPacket)
                .andThen(new MaxMessageSizeFilter<>(csmStorage))
                .then(sender);

        // INBOUND
        Handler<CoapRequest, CoapResponse> inboundService = inboundFilter
                .andThen(new RescueFilter())
                .andThenIf(hasRoute(), new CriticalOptionVerifier(messaging.getRecognizedCustomOptions()))
                .andThenIf(hasRoute(), new BlockWiseIncomingFilter(capabilities(), messaging.getMaxIncomingBlockTransferSize()))
                .andThen(routeFilter)
                .then(handler);

        // OUTBOUND
        TcpExchangeFilter exchangeFilter = new TcpExchangeFilter();
        Handler<CoapRequest, CoapResponse> outboundHandler = outboundFilter
                .andThen(new ObserveRequestFilter(observationsStore::add))
                .andThen(new CongestionControlFilter<>(messaging.getQueueMaxSize(), CoapRequest::getPeerAddress))
                .andThen(new BlockWiseOutgoingFilter(capabilities(), messaging.getMaxIncomingBlockTransferSize()))
                .andThen(exchangeFilter)
                .andThenMap(CoapTcpPacketConverter::toCoapPacket)
                .then(sender);

        Function<SeparateResponse, Boolean> inboundObservation = new ObservationHandler(observations.getReceiver(), observationsStore)
                .andThen(it -> it.getNow(true));

        CoapTcpDispatcher dispatcher = new CoapTcpDispatcher(
                sender,
                csmStorage,
                new Capabilities(messaging.getMaxMessageSize(), messaging.getBlockSize() != null),
                inboundService,
                exchangeFilter::handleResponse,
                inboundObservation
        );

        coapTransport.setListener(dispatcher);

        return new CoapServer(coapTransport, dispatcher::handle, outboundHandler, sendNotification, Function::identity);
    }

    private boolean hasRoute() {
        return !Objects.equals(handler, RoutingHandler.NOT_FOUND);
    }
}
