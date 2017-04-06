/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.function.wiretap;

import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

/**
 * @author Dave Syer
 *
 */
public class DefaultBridge<T> implements Bridge<T> {

    private final Processor<T, T> emitter;
    private final Flux<T> sink;

    public DefaultBridge() {
        this.emitter = emitter().get();
        this.sink = broadcaster().apply(emitter);
    }

    @Override
    public void send(T item) {
        emitter.onNext(item);
    }

    @Override
    public Flux<T> receive() {
        return sink;
    }

    protected Supplier<Processor<T, T>> emitter() {
        return () -> UnicastProcessor.<T>create().serialize();
    }

    protected Function<Publisher<T>, Flux<T>> broadcaster() {
        return flux -> Flux.from(flux).replay().autoConnect();
    }
}
