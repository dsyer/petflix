/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.samples.petclinic.owner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author Dave Syer
 */
@RestController
class VideoController {
    private RestTemplate template;

    VideoController(RestTemplateBuilder builder) {
        template = builder.build();
    }

    @GetMapping("/owners/videos/{id}")
    public Map<String, String> video(@PathVariable Integer id) throws Exception {
        return template.exchange(
                RequestEntity.post(new URI("http://localhost:9000/videos"))
                        .accept(MediaType.APPLICATION_JSON).body(Arrays.asList(id)),
                new ParameterizedTypeReference<List<Map<String, String>>>() {
                }).getBody().iterator().next();
    }
}
