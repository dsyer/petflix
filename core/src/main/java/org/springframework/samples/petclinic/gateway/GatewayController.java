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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.cloud.function.gateway.ProxyExchange;
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
class GatewayController extends WebMvcConfigurerAdapter {

    @Value("${services.video.uri}")
    private URI videosUrl;

    @Value("${services.commander.uri}")
    private URI commanderUrl;

    @Autowired
    private ResourceProperties resources;

    @GetMapping("/videos/{id}")
    public ResponseEntity<?> videos(@PathVariable Integer id, ProxyExchange<?> proxy)
            throws Exception {
        return proxy.uri(videosUrl.toString() + "/videos/" + id).get();
    }

    @PostMapping("/commands/{action}")
    public ResponseEntity<?> rater(@PathVariable String action,
            @RequestBody Map<String, Object> request,
            ProxyExchange<List<Map<String, Object>>> proxy) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("action", action);
        body.put("timestamp", new Date());
        body.put("data", request);
        return proxy.uri(commanderUrl.toString() + "/commands").body(Arrays.asList(body))
                .post(this::event);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/videos/**")
                .addResourceLocations(videosUrl.toString() + "/resources/")
                .resourceChain(resources.getChain().isCache()).addResolver(
                        new VersionResourceResolver().addContentVersionStrategy("/**"));
    }

    private ResponseEntity<Map<String, Object>> event(
            ResponseEntity<List<Map<String, Object>>> result) {
        Map<String, Object> body;
        body = new HashMap<>();
        if (!result.getBody().isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getBody().iterator()
                    .next().get("data");
            body.putAll(data);
        }
        return ResponseEntity.status(result.getStatusCode()).headers(result.getHeaders())
                .body(body);
    }

}
