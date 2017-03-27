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
package org.springframework.samples.petclinic.gateway;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.cloud.function.web.ProxyExchange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

/**
 * @author Dave Syer
 */
@RestController
class VideoController extends WebMvcConfigurerAdapter {

    @Value("${services.video.uri}")
    private URI videosUrl;

    @Autowired
    private ResourceProperties resources;

    @GetMapping("/owners/videos/{id}")
    public ResponseEntity<?> videos(@PathVariable Integer id, ProxyExchange<?> proxy)
            throws Exception {
        return proxy.uri(videosUrl.toString() + "/videos/" + id).get();
    }

    @PostMapping("/owners/videos/{id}")
    public ResponseEntity<?> rater(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, ProxyExchange<List<Object>> proxy) throws Exception {
        body.put("id", id);
        return proxy.uri(videosUrl.toString() + "/ratings").body(Arrays.asList(body))
                .post(this::first);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/owners/videos/**")
                .addResourceLocations(videosUrl.toString() + "/resources/")
                .resourceChain(resources.getChain().isCache()).addResolver(
                        new VersionResourceResolver().addContentVersionStrategy("/**"));
    }

    private ResponseEntity<Object> first(ResponseEntity<List<Object>> result) {
        Object body;
        if (result.getBody().isEmpty()) {
            body = "";
        }
        else {
            body = result.getBody().iterator().next();
        }
        return ResponseEntity.status(result.getStatusCode()).headers(result.getHeaders())
                .body(body);
    }

}
