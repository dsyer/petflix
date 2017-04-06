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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Processor;

import reactor.core.publisher.Flux;

/**
 * @author Dave Syer
 *
 */
public class DefaultBridge<T> implements Bridge<T> {

    private final Processor<T, T> emitter;
    private final Flux<T> sink;

    public DefaultBridge(Processor<T, T> emitter) {
        this.emitter = emitter;
        this.sink = broadcaster().apply(emitter);
    }

    @Override
    public Consumer<T> consumer() {
        return object -> emitter.onNext(object);
    }

    @Override
    public Supplier<Flux<T>> supplier() {
        return () -> sink;
    }

}
