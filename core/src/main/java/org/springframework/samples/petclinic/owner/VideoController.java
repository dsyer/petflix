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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author Dave Syer
 */
@RestController
class VideoController {
    private RestTemplate template;

    @Value("${services.video.uri}")
    private URI videosUrl;

    VideoController(RestTemplateBuilder builder) {
        template = builder.build();
    }

    @GetMapping("/owners/videos/{id}")
    public Map<String, Object> video(@PathVariable Integer id) throws Exception {
        String path = videosUrl.toString() + "/videos";
        Integer body = id;
        return post(body, path);
    }

    @PostMapping("/owners/videos/{id}")
    public Map<String, Object> rate(@PathVariable Integer id,
            @RequestBody Map<String, Object> body) throws Exception {
        body = new LinkedHashMap<>(body);
        body.put("id", id);
        String path = videosUrl.toString() + "/ratings";
        return post(body, path);
    }

    private Map<String, Object> post(Object body, String path) throws URISyntaxException {
        return template.exchange(
                RequestEntity.post(new URI(path)).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Arrays.asList(body)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody().iterator().next();
    }
}
