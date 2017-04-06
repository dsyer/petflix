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

import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.wiretap.Bridge;
import org.springframework.cloud.function.wiretap.DefaultBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MultipleWiretapConfigurationTests {

    @Autowired
    private Bridge<Command> wiretap;

    @Autowired
    private ApplicationContext context;

    @Test
    public void test() {
        assertThat(wiretap).isNotNull();
        assertThat(context.getBeanNamesForType(Consumer.class)).hasSize(2);
    }

    @TestConfiguration
    protected static class ExtraWiretap {
        @Bean
        public Bridge<Foo> fooBridge() {
            return new DefaultBridge<>();
        }
    }

    protected static class Foo {
    }
}
