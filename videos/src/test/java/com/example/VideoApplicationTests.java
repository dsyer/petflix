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
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.head;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class VideoApplicationTests {

    @Autowired
    private MockMvc rest;

    @Test
    public void home() throws Exception {
        rest.perform(get("/resources/templates/pet.html"))
                .andExpect(content()
                        .string(containsString("ng-repeat=\"star in pet.stars\"")))
                .andDo(document("pet"));
    }

    @Test
    public void scripts() throws Exception {
        rest.perform(get("/resources/js/app.js"))
                .andExpect(content().string(containsString("$http")))
                .andDo(document("app"));
    }

    @Test
    public void homeHead() throws Exception {
        rest.perform(head("/resources/templates/pet.html"))
                .andDo(document("petHead"));
    }

    @Test
    public void scriptsHead() throws Exception {
        rest.perform(head("/resources/js/app.js"))
                .andDo(document("appHead"));
    }

    @Test
    public void video() throws Exception {
        rest.perform(get("/videos/0").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("https://www.youtube.com")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("video"));
    }

    @Test
    public void videos() throws Exception {
        rest.perform(post("/videos").content("[1]").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("https://www.youtube.com")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("videos"));
    }

}
