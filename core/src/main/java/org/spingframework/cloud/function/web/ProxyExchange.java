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

package org.spingframework.cloud.function.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * @author Dave Syer
 *
 */
public class ProxyExchange {

    private static final ParameterizedTypeReference<List<Object>> LIST_TYPE = new ParameterizedTypeReference<List<Object>>() {
    };

    private URI uri;

    private RestTemplate rest;

    private Object body;

    private RequestResponseBodyMethodProcessor delegate;

    private NativeWebRequest webRequest;

    private ModelAndViewContainer mavContainer;

    private WebDataBinderFactory binderFactory;

    private HttpHeaders headers = new HttpHeaders();

    public ProxyExchange(RestTemplate rest, NativeWebRequest webRequest,
            ModelAndViewContainer mavContainer, WebDataBinderFactory binderFactory) {
        this.rest = rest;
        this.webRequest = webRequest;
        this.mavContainer = mavContainer;
        this.binderFactory = binderFactory;
        this.delegate = new RequestResponseBodyMethodProcessor(
                rest.getMessageConverters());
    }

    public ProxyExchange body(Object body) {
        this.body = body;
        return this;
    }

    public ProxyExchange header(String name, String... value) {
        this.headers.put(name, Arrays.asList(value));
        return this;
    }

    public ProxyExchange headers(HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public String path() {
        return (String) this.webRequest.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
                WebRequest.SCOPE_REQUEST);
    }
    public String path(String prefix) {
        return path().substring(prefix.length());
    }

    public ProxyExchange uri(String uri) {
        try {
            this.uri = new URI(uri);
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot create URI", e);
        }
        return this;
    }

    public ResponseEntity<Object> get() {
        return rest.exchange(headers((BodyBuilder) RequestEntity.get(uri)).build(),
                Object.class);
    }

    public ResponseEntity<Object> getFirst() {
        ResponseEntity<List<Object>> result = rest.exchange(
                headers((BodyBuilder) RequestEntity.get(uri)).build(), LIST_TYPE);
        return first(result);
    }

    public ResponseEntity<Object> post() {
        return rest.exchange(headers(RequestEntity.post(uri)).body(body()), Object.class);
    }

    public ResponseEntity<Object> postFirst() {
        ResponseEntity<List<Object>> result = rest
                .exchange(headers(RequestEntity.post(uri)).body(body()), LIST_TYPE);
        return first(result);
    }

    public ResponseEntity<Object> delete() {
        return rest.exchange(headers((BodyBuilder) RequestEntity.delete(uri)).build(),
                Object.class);
    }

    public ResponseEntity<Object> put() {
        return rest.exchange(headers(RequestEntity.put(uri)).body(body()), Object.class);
    }

    public ResponseEntity<Object> putFirst() {
        ResponseEntity<List<Object>> result = rest
                .exchange(headers(RequestEntity.put(uri)).body(body()), LIST_TYPE);
        return first(result);
    }

    private BodyBuilder headers(BodyBuilder builder) {
        for (String name : headers.keySet()) {
            builder.header(name, headers.get(name).toArray(new String[0]));
        }
        return builder;
    }

    private Object body() {
        if (body != null) {
            return body;
        }
        return getRequestBody();
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

    private Object getRequestBody() {
        MethodParameter input = new MethodParameter(
                ClassUtils.getMethod(BodyGrabber.class, "body", Object.class), 0);
        try {
            delegate.resolveArgument(input, mavContainer, webRequest, binderFactory);
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot resolve body", e);
        }
        String name = Conventions.getVariableNameForParameter(input);
        BindingResult result = (BindingResult) mavContainer.getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + name);
        Object body = result.getModel().get(name);
        return body;
    }

    protected static class BodyGrabber {
        public Object body(@RequestBody Object body) {
            return body;
        }
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }
}
