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
import java.util.Map;

import org.spingframework.cloud.function.web.ProxyExchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Dave Syer
 */
@RestController
class VideoController {

    @Value("${services.video.uri}")
    private URI videosUrl;

    @GetMapping("/owners/videos/{id}")
    public ResponseEntity<?> videos(@PathVariable Integer id, ProxyExchange proxy)
            throws Exception {
        return proxy.uri(videosUrl.toString() + "/videos/" + id).get();
    }

    @PostMapping("/owners/videos/{id}")
    public ResponseEntity<?> rater(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, ProxyExchange proxy) throws Exception {
        body.put("id", id);
        return proxy.uri(videosUrl.toString() + "/ratings").body(Arrays.asList(body))
                .postFirst();
    }

}
