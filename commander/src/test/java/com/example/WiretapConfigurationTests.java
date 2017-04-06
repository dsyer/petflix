/*
 * Copyright 2012-2015 the original author or authors.
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

package com.example;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.wiretap.Bridge;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class WiretapConfigurationTests {

    @Autowired
    private Bridge<Command> wiretap;

    @Test
    public void test() {
        Flux<Command> flux = wiretap.receive();
        // @formatter:off
        StepVerifier.create(flux.log())
            .then(() -> wiretap.send(new Command())) 
            .expectNextCount(1)
            .thenCancel()
            .verify(Duration.ofMillis(500L));
        // @formatter:on
    }

    @Test
    public void again() {
        Flux<Command> flux = wiretap.receive();
        // @formatter:off
        StepVerifier.create(flux.log())
            .then(() -> wiretap.send(new Command())) 
            .expectNextCount(1)
            .thenCancel()
            .verify(Duration.ofMillis(500L));
        // @formatter:on
    }

}
