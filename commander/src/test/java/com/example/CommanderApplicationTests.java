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

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.cloud.contract.wiremock.restdocs.WireMockRestDocs.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import reactor.core.publisher.Flux;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class CommanderApplicationTests {

    @Autowired
    private MockMvc rest;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CommanderApplication application;

    @Test
    public void commands() throws Exception {
        rest.perform(post("/commands")
                .content(mapper.writeValueAsString(Arrays.asList(new Command("rate-video")
                        .data(Collections.singletonMap("stars", 2)))))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"id\"")))
                .andDo(verify().jsonPath("$[0].data.stars")
                        .jsonPath("$[0].action", "rate-video").stub("commands"));
    }

    @Test
    public void getCommandById() throws Exception {
        application.commands().accept(Flux.just(
                new Command("rate-video").data(Collections.singletonMap("stars", 2))));
        String id = application.replay().get().blockFirst().getId();
        rest.perform(get("/store/commands/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"id\"")))
                .andDo(document("command"));
    }

    @Test
    public void getEventById() throws Exception {
        application.commands().accept(Flux.just(
                new Command("rate-video").data(Collections.singletonMap("stars", 3))));
        String command = application.replay().get().blockFirst().getId();
        application.events().accept(Flux.just(new Event("rated-video")
                .data(Collections.singletonMap("stars", 3)).parent(command)));
        String id = application.eventSupplier().get().blockFirst().getId();
        rest.perform(get("/store/events/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"id\"")))
                .andDo(document("event"));
    }

}
