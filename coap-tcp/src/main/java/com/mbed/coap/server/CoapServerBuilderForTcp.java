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

import static java.util.Objects.requireNonNull;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.CoapTcpPacketConverter;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.block.BlockWiseIncomingFilter;
import com.mbed.coap.server.block.BlockWiseNotificationFilter;
import com.mbed.coap.server.block.BlockWiseOutgoingFilter;
import com.mbed.coap.server.filter.CongestionControlFilter;
import com.mbed.coap.server.messaging.Capabilities;
import com.mbed.coap.server.messaging.CapabilitiesStorage;
import com.mbed.coap.server.messaging.CapabilitiesStorageImpl;
import com.mbed.coap.server.messaging.CoapTcpDispatcher;
import com.mbed.coap.server.messaging.PayloadSizeVerifier;
import com.mbed.coap.server.messaging.TcpExchangeFilter;
import com.mbed.coap.server.observe.NotificationsReceiver;
import com.mbed.coap.server.observe.ObservationsStore;
import com.mbed.coap.transport.CoapTcpTransport;
import com.mbed.coap.transport.LoggingCoapTransport;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Function;

public class CoapServerBuilderForTcp {
    private CoapTcpTransport coapTransport;
    private Service<CoapRequest, CoapResponse> route = RouterService.NOT_FOUND_SERVICE;
    private int maxMessageSize = 1152; //default
    private CapabilitiesStorage csmStorage;
    private int maxIncomingBlockTransferSize = 10_000_000; //default to 10 MB
    private int maxQueueSize = 100;
    private BlockSize blockSize;
    private Filter.SimpleFilter<CoapRequest, CoapResponse> outboundFilter = Filter.identity();
    private Filter.SimpleFilter<CoapRequest, CoapResponse> routeFilter = Filter.identity();
    private NotificationsReceiver notificationsReceiver = NotificationsReceiver.REJECT_ALL;
    private ObservationsStore observationsStore = ObservationsStore.ALWAYS_EMPTY;
    private Boolean isTransportLoggingEnabled = true;

    CoapServerBuilderForTcp() {
        csmStorage = new CapabilitiesStorageImpl();
    }

    private CapabilitiesStorage capabilities() {
        return csmStorage;
    }

    public final CoapServerBuilderForTcp blockSize(BlockSize blockSize) {
        this.blockSize = blockSize;
        return this;
    }

    public CoapServerBuilderForTcp transport(CoapTcpTransport coapTransport) {
        this.coapTransport = requireNonNull(coapTransport);
        return this;
    }

    public CoapServerBuilderForTcp route(Service<CoapRequest, CoapResponse> route) {
        this.route = route;
        return this;
    }

    public CoapServerBuilderForTcp route(RouterService.RouteBuilder routeBuilder) {
        return route(routeBuilder.build());
    }

    public CoapServerBuilderForTcp routeFilter(Filter.SimpleFilter<CoapRequest, CoapResponse> routeFilter) {
        this.routeFilter = requireNonNull(routeFilter);
        return this;
    }

    public CoapServerBuilderForTcp csmStorage(CapabilitiesStorage csmStorage) {
        this.csmStorage = csmStorage;
        return this;
    }

    public CoapServerBuilderForTcp maxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        return this;
    }

    public CoapServerBuilderForTcp maxIncomingBlockTransferSize(int size) {
        this.maxIncomingBlockTransferSize = size;
        return this;
    }

    public CoapServerBuilderForTcp queueMaxSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public CoapServerBuilderForTcp outboundFilter(Filter.SimpleFilter<CoapRequest, CoapResponse> outboundFilter) {
        this.outboundFilter = outboundFilter;
        return this;
    }

    public CoapServerBuilderForTcp notificationsReceiver(NotificationsReceiver notificationsReceiver) {
        this.notificationsReceiver = requireNonNull(notificationsReceiver);
        if (observationsStore.equals(ObservationsStore.ALWAYS_EMPTY)) {
            return observationsStore(ObservationsStore.inMemory());
        }
        return this;
    }

    public CoapServerBuilderForTcp observationsStore(ObservationsStore observationsStore) {
        this.observationsStore = requireNonNull(observationsStore);
        return this;
    }

    public CoapServerBuilderForTcp transportLogging(Boolean transportLogging) {
        this.isTransportLoggingEnabled = requireNonNull(transportLogging);
        return this;
    }

    public CoapClient buildClient(InetSocketAddress target) throws IOException {
        return CoapClient.create(target, build().start(), r -> r.getCode() == Code.C703_PONG);
    }

    public CoapServer build() {
        Service<CoapPacket, Boolean> sender = (isTransportLoggingEnabled ? LoggingCoapTransport.wrap(coapTransport) : coapTransport)::sendPacket;

        // NOTIFICATION
        Service<SeparateResponse, Boolean> sendNotification = new NotificationValidator()
                .andThen(new BlockWiseNotificationFilter(capabilities()))
                .andThenMap(CoapTcpPacketConverter::toCoapPacket)
                .andThen(new PayloadSizeVerifier<>(csmStorage))
                .then(sender);

        // INBOUND
        Service<CoapRequest, CoapResponse> inboundService = new RescueFilter()
                .andThenIf(hasRoute(), new CriticalOptionVerifier())
                .andThenIf(hasRoute(), new BlockWiseIncomingFilter(capabilities(), maxIncomingBlockTransferSize))
                .andThen(routeFilter)
                .then(route);

        // OUTBOUND
        TcpExchangeFilter exchangeFilter = new TcpExchangeFilter();
        Service<CoapRequest, CoapResponse> outboundService = outboundFilter
                .andThen(new ObserveRequestFilter(observationsStore::add))
                .andThen(new CongestionControlFilter<>(maxQueueSize, CoapRequest::getPeerAddress))
                .andThen(new BlockWiseOutgoingFilter(capabilities(), maxIncomingBlockTransferSize))
                .andThen(exchangeFilter)
                .andThenMap(CoapTcpPacketConverter::toCoapPacket)
                .then(sender);

        Function<SeparateResponse, Boolean> inboundObservation = new ObservationHandler(notificationsReceiver, observationsStore)
                .andThen(it -> it.getNow(true));

        CoapTcpDispatcher dispatcher = new CoapTcpDispatcher(
                sender,
                csmStorage,
                new Capabilities(maxMessageSize, blockSize != null),
                inboundService,
                exchangeFilter::handleResponse,
                inboundObservation
        );

        coapTransport.setListener(dispatcher);

        return new CoapServer(coapTransport, dispatcher::handle, outboundService, sendNotification, Function::identity);
    }

    private boolean hasRoute() {
        return !Objects.equals(route, RouterService.NOT_FOUND_SERVICE);
    }
}
