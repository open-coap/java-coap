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
package opencoap.endpoint.pipeline;

import static java.util.Objects.requireNonNull;
import static opencoap.util.FutureHelpers.become;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import opencoap.core.CoapTimeoutException;
import opencoap.core.Filter;
import opencoap.core.Handler;
import opencoap.endpoint.RetransmissionBackOff;
import opencoap.util.Scheduler;

public final class RetransmissionFilter<REQ, RES> implements Filter<REQ, RES> {

    private final Scheduler scheduler;
    private final RetransmissionBackOff backoff;
    private final Predicate<REQ> doRetransmit;

    public RetransmissionFilter(Scheduler scheduler, RetransmissionBackOff backoff, Predicate<REQ> doRetransmit) {
        this.scheduler = requireNonNull(scheduler);
        this.backoff = requireNonNull(backoff);
        this.doRetransmit = requireNonNull(doRetransmit);
    }

    @Override
    public CompletableFuture<RES> apply(REQ request, Handler<REQ, RES> service) {
        CompletableFuture<RES> promise = service.apply(request);

        if (!doRetransmit.test(request)) {
            return promise;
        }

        Runnable cancel = scheduler.schedule(backoff.next(1), () -> next(promise, 2, () -> service.apply(request)));
        promise.whenComplete((__, ex) -> cancel.run());

        return promise;
    }

    private void next(CompletableFuture<RES> promise, int attempt, Supplier<CompletableFuture<RES>> retryFunc) {
        Duration delay = backoff.next(attempt);
        if (!delay.isZero()) {
            become(promise, retryFunc.get());
            Runnable cancel = scheduler.schedule(delay, () -> next(promise, attempt + 1, retryFunc));

            promise.whenComplete((__, err) -> cancel.run());
        } else {
            promise.completeExceptionally(new CoapTimeoutException());
        }
    }
}
