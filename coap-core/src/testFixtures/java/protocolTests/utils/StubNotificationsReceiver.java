/*
 * Copyright (C) 2022-2023 java-coap contributors (https://github.com/open-coap/java-coap)
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
package protocolTests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.observe.NotificationsReceiver;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class StubNotificationsReceiver implements NotificationsReceiver {

    final BlockingQueue<SeparateResponse> queue = new LinkedBlockingQueue<>();

    @Override
    public boolean onObservation(String resourceUriPath, SeparateResponse obs) {
        queue.add(obs);
        return true;
    }

    public SeparateResponse take() throws InterruptedException {
        return queue.poll(5, TimeUnit.SECONDS);
    }

    public void verifyReceived(CoapResponse obs) throws InterruptedException {
        CoapResponse received = queue.poll(1, TimeUnit.SECONDS).asResponse();
        assertEquals(obs, received);
    }

    public boolean noMoreReceived() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }
}
