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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class VideoApplicationTests {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate rest;

    @Test
    public void home() {
        assertThat(rest.getForObject("/resources/templates/pet.html", String.class)).contains("ng-repeat=\"star in pet.stars\"");
    }

    @Test
    public void scripts() {
        assertThat(rest.getForObject("/resources/js/app.js", String.class)).contains("$http");
    }

    @Test
    public void post() {
        assertThat(rest.postForObject("/videos", 1, String.class))
                .contains("https://www.youtube.com");
    }

    @Test
    public void ratings() {
        assertThat(rest.postForObject("/ratings", new Rating(), String.class))
                .contains("https://www.youtube.com");
    }

    @Test
    public void wiretap() {
        rest.postForObject("/ratings", new Rating(), String.class);
        ResponseEntity<String> result = rest.getForEntity("/ratingSupplier", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).contains("stars");
    }

    @Test
    public void twice() {
        rest.postForObject("/ratings", new Rating(), String.class);
        ResponseEntity<String> result = rest.getForEntity("/ratingSupplier", String.class);
        rest.postForObject("/ratings", new Rating(), String.class);
        result = rest.getForEntity("/ratingSupplier", String.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).contains("stars");
    }

}
