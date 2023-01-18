/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
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
package protocolTests;

import static com.mbed.coap.packet.CoapRequest.get;
import static com.mbed.coap.packet.CoapRequest.observe;
import static com.mbed.coap.packet.Opaque.EMPTY;
import static com.mbed.coap.packet.Opaque.of;
import static com.mbed.coap.server.observe.NotificationsReceiver.REJECT_ALL;
import static com.mbed.coap.transmission.RetransmissionBackOff.ofFixed;
import static com.mbed.coap.transport.udp.DatagramSocketTransport.udp;
import static java.time.Duration.ofMillis;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.client.CoapClient;
import com.mbed.coap.packet.BlockSize;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.RouterService;
import com.mbed.coap.server.observe.ObserversManager;
import com.mbed.coap.utils.ObservableResource;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocolTests.utils.StubNotificationsReceiver;


public class ObservationTest {

    private final String RES_OBS_PATH1 = "/obs/path1";
    private CoapServer server;
    private InetSocketAddress SERVER_ADDRESS;
    private ObservableResource obsResource;
    private final ObserversManager observersManager = new ObserversManager();
    private final Opaque token1001 = Opaque.ofBytes(0x10, 0x01);

    @BeforeEach
    public void setUpClass() throws Exception {
        obsResource = new ObservableResource(RES_OBS_PATH1, CoapResponse.ok(EMPTY), observersManager);
        server = CoapServer.builder().transport(udp())
                .route(RouterService.builder()
                        .get("/path1", __ -> CoapResponse.ok("content1").toFuture())
                        .get(RES_OBS_PATH1, obsResource)
                )
                .retransmission(ofFixed(ofMillis(500)))
                .responseTimeout(ofMillis(600))
                .blockSize(BlockSize.S_128).build();

        observersManager.init(server);
        server.start();
        SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", server.getLocalSocketAddress().getPort());
    }

    @AfterEach
    public void tearDownClass() throws Exception {
        server.stop();
    }

    @Test
    public void observationTest() throws Exception {
        StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
        CoapClient client = CoapServer.builder().transport(udp()).notificationsReceiver(notifReceiver).buildClient(SERVER_ADDRESS);

        client.sendSync(observe(RES_OBS_PATH1).token(token1001));

        //notify 1
        obsResource.putPayload(of("duupa"));

        CoapResponse packet = notifReceiver.take().asResponse();
        assertEquals("duupa", packet.getPayloadString());
        assertEquals(Integer.valueOf(1), packet.options().getObserve());

        //notify 2
        obsResource.putPayload(of("duupa2"));

        packet = notifReceiver.take().asResponse();
        assertEquals("duupa2", packet.getPayloadString());
        assertEquals(Integer.valueOf(2), packet.options().getObserve());

        //notify 3 with NON-CONF
        System.out.println("\n-- notify 3 with NON");
        // OBS_RESOURCE_1.setConfirmNotification(false);
        obsResource.putPayload(of("duupa3"));

        packet = notifReceiver.take().asResponse();
        assertEquals("duupa3", packet.getPayloadString());
        assertEquals(Integer.valueOf(3), packet.options().getObserve());
        // OBS_RESOURCE_1.setConfirmNotification(true);

        //refresh observation
        assertEquals(1, obsResource.observationRelations());
        client.sendSync(observe(RES_OBS_PATH1));

        assertEquals(1, obsResource.observationRelations());
        client.close();
    }

    @Test
    public void terminateObservationByServerWithErrorCode() throws Exception {
        StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
        CoapClient client = CoapServer.builder().transport(udp()).notificationsReceiver(notifReceiver).buildClient(SERVER_ADDRESS);

        client.sendSync(observe(RES_OBS_PATH1).token(token1001));

        obsResource.putPayload(of("duupabb"));
        CoapResponse packet = notifReceiver.take().asResponse();

        assertEquals("duupabb", packet.getPayloadString());

        obsResource.terminate(Code.C404_NOT_FOUND);
        assertEquals(0, obsResource.observationRelations(), "Number of observation did not change");

        client.close();
    }

    @Test
    public void terminateObservationByServerTimeout() throws Exception {
        StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
        CoapClient client = CoapServer.builder().transport(udp()).notificationsReceiver(notifReceiver).buildClient(SERVER_ADDRESS);

        client.sendSync(observe(RES_OBS_PATH1).token(token1001));
        client.close();

        obsResource.putPayload(of("duupabb")); //make notification

        await().untilAsserted(() ->
                assertEquals(0, obsResource.observationRelations(), "Observation did not terminate")
        );
    }

    @Test
    public void dontTerminateObservationIfNoObs() throws Exception {
        StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
        CoapClient client = CoapServer.builder().transport(udp()).notificationsReceiver(notifReceiver).buildClient(SERVER_ADDRESS);

        //register observation
        client.sendSync(observe(RES_OBS_PATH1).token(token1001));

        //notify
        obsResource.putPayload(of("keho"));

        //do not terminate observation by doing get
        client.sendSync(get(RES_OBS_PATH1));

        await().untilAsserted(() ->
                assertEquals(1, obsResource.observationRelations(), "Observation terminated")
        );

        client.close();
    }

    @Test
    public void terminateObservationByClientWithRst() throws Exception {
        CoapClient client = CoapServer.builder().transport(udp()).notificationsReceiver(REJECT_ALL).buildClient(SERVER_ADDRESS);

        //register observation
        client.sendSync(observe(RES_OBS_PATH1).token(token1001));

        //notify
        obsResource.putPayload(of("keho"));

        await().untilAsserted(() ->
                assertEquals(0, obsResource.observationRelations(), "Observation not terminated")
        );
        obsResource.putPayload(of("keho"));

        client.close();
    }

    @Test
    public void observationWithBlocks() throws Exception {
        obsResource.putPayload(ClientServerWithBlocksTest.BIG_RESOURCE);

        StubNotificationsReceiver notifReceiver = new StubNotificationsReceiver();
        CoapClient client = CoapServer.builder().transport(udp()).blockSize(BlockSize.S_128).notificationsReceiver(notifReceiver).buildClient(SERVER_ADDRESS);

        //register observation
        CoapResponse msg = client.sendSync(observe(RES_OBS_PATH1));
        assertEquals(ClientServerWithBlocksTest.BIG_RESOURCE, msg.getPayload());

        //notif 1
        System.out.println("\n-- NOTIF 1");
        obsResource.putPayload(ClientServerWithBlocksTest.BIG_RESOURCE.concat(of("change-1")));
        CoapResponse packet = notifReceiver.take().asResponse();
        assertEquals(ClientServerWithBlocksTest.BIG_RESOURCE.slice(0, 128), packet.getPayload());
        //assertEquals(Integer.valueOf(1), packet.headers().getObserve());

        client.close();
    }

}
