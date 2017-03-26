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

package org.springframework.cloud.function.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.util.AbstractUriTemplateHandler;

/**
 * @author Dave Syer
 *
 */
public class ProxyExchange<T> {

    public static Set<String> DEFAULT_SENSITIVE = new HashSet<>(
            Arrays.asList("cookie", "authorization"));

    private static final ParameterizedTypeReference<List<Object>> LIST_TYPE = new ParameterizedTypeReference<List<Object>>() {
    };

    private URI uri;

    private NestedTemplate rest;

    private Object body;

    private RequestResponseBodyMethodProcessor delegate;

    private NativeWebRequest webRequest;

    private ModelAndViewContainer mavContainer;

    private WebDataBinderFactory binderFactory;

    private Set<String> sensitive;

    private HttpHeaders headers = new HttpHeaders();

    private Type type;

    public ProxyExchange(RestTemplate rest, NativeWebRequest webRequest,
            ModelAndViewContainer mavContainer, WebDataBinderFactory binderFactory,
            Type type) {
        this.type = type;
        this.rest = createTemplate(rest);
        this.webRequest = webRequest;
        this.mavContainer = mavContainer;
        this.binderFactory = binderFactory;
        this.delegate = new RequestResponseBodyMethodProcessor(
                rest.getMessageConverters());
    }

    private NestedTemplate createTemplate(RestTemplate input) {
        NestedTemplate rest = new NestedTemplate();
        rest.setMessageConverters(input.getMessageConverters());
        rest.setErrorHandler(input.getErrorHandler());
        rest.setDefaultUriVariables(
                ((AbstractUriTemplateHandler) input.getUriTemplateHandler())
                        .getDefaultUriVariables());
        rest.setRequestFactory(input.getRequestFactory());
        rest.setInterceptors(input.getInterceptors());
        return rest;
    }

    public ProxyExchange<T> body(Object body) {
        this.body = body;
        return this;
    }

    public ProxyExchange<T> header(String name, String... value) {
        this.headers.put(name, Arrays.asList(value));
        return this;
    }

    public ProxyExchange<T> headers(HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public ProxyExchange<T> sensitive(String... names) {
        if (this.sensitive == null) {
            this.sensitive = new HashSet<>();
        }
        for (String name : names) {
            this.sensitive.add(name.toLowerCase());
        }
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

    public ProxyExchange<T> uri(String uri) {
        try {
            this.uri = new URI(uri);
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot create URI", e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <S> ProxyExchange<S> expect(ParameterizedTypeReference<S> type) {
        this.type = type.getType();
        return (ProxyExchange<S>) this;
    }

    public void forward(String handler) {
        HttpServletRequest request = this.webRequest
                .getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = this.webRequest
                .getNativeResponse(HttpServletResponse.class);
        try {
            request.getRequestDispatcher(handler).forward(
                    new BodyForwardingHttpServletRequest(request, response), response);
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot forward request", e);
        }
    }

    public ResponseEntity<T> get() {
        RequestEntity<?> requestEntity = headers((BodyBuilder) RequestEntity.get(uri))
                .build();
        Type type = this.type;
        if (!ClassUtils.isPresent(type.getTypeName(), null)) {
            type = Object.class;
        }
        RequestCallback requestCallback = rest.httpEntityCallback((Object) requestEntity,
                type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = rest
                .responseEntityExtractor(type);
        return rest.execute(requestEntity.getUrl(), requestEntity.getMethod(),
                requestCallback, responseExtractor);
    }

    public ResponseEntity<?> getFirst() {
        ResponseEntity<List<Object>> result = rest.exchange(
                headers((BodyBuilder) RequestEntity.get(uri)).build(), LIST_TYPE);
        return first(result);
    }

    public ResponseEntity<T> post() {
        return rest.exchange(headers(RequestEntity.post(uri)).body(body()),
                new ParameterizedTypeReference<T>() {
                });
    }

    public ResponseEntity<?> postFirst() {
        ResponseEntity<List<Object>> result = rest
                .exchange(headers(RequestEntity.post(uri)).body(body()), LIST_TYPE);
        return first(result);
    }

    public ResponseEntity<T> delete() {
        return rest.exchange(headers((BodyBuilder) RequestEntity.delete(uri)).build(),
                new ParameterizedTypeReference<T>() {
                });
    }

    public ResponseEntity<T> put() {
        return rest.exchange(headers(RequestEntity.put(uri)).body(body()),
                new ParameterizedTypeReference<T>() {
                });
    }

    public ResponseEntity<?> putFirst() {
        ResponseEntity<List<Object>> result = rest
                .exchange(headers(RequestEntity.put(uri)).body(body()), LIST_TYPE);
        return first(result);
    }

    private BodyBuilder headers(BodyBuilder builder) {
        Set<String> sensitive = this.sensitive;
        if (sensitive == null) {
            sensitive = DEFAULT_SENSITIVE;
        }
        for (String name : headers.keySet()) {
            if (sensitive.contains(name.toLowerCase())) {
                continue;
            }
            builder.header(name, headers.get(name).toArray(new String[0]));
        }
        return builder;
    }

    private Object body() {
        if (body != null) {
            return body;
        }
        body = getRequestBody();
        return body;
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
        for (String key : mavContainer.getModel().keySet()) {
            if (key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
                BindingResult result = (BindingResult) mavContainer.getModel().get(key);
                return result.getTarget();
            }
        }
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
        return result.getTarget();
    }

    class NestedTemplate extends RestTemplate {
        @Override
        protected <S> RequestCallback httpEntityCallback(Object requestBody,
                Type responseType) {
            return super.httpEntityCallback(requestBody, responseType);
        }

        @Override
        protected <S> ResponseExtractor<ResponseEntity<S>> responseEntityExtractor(
                Type responseType) {
            return super.responseEntityExtractor(responseType);
        }
    }

    class BodyForwardingHttpServletRequest extends HttpServletRequestWrapper {
        private HttpServletRequest request;
        private HttpServletResponse response;

        BodyForwardingHttpServletRequest(HttpServletRequest request,
                HttpServletResponse response) {
            super(request);
            this.request = request;
            this.response = response;
        }

        private List<String> header(String name) {
            List<String> list = headers.get(name);
            return list;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            Object body = body();
            MethodParameter output = new MethodParameter(
                    ClassUtils.getMethod(BodySender.class, "body"), -1);
            ServletOutputToInputConverter response = new ServletOutputToInputConverter(
                    this.response);
            ServletWebRequest webRequest = new ServletWebRequest(this.request, response);
            try {
                delegate.handleReturnValue(body, output, mavContainer, webRequest);
            }
            catch (HttpMessageNotWritableException
                    | HttpMediaTypeNotAcceptableException e) {
                throw new IllegalStateException("Cannot convert body");
            }
            return response.getInputStream();
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = headers.keySet();
            if (names.isEmpty()) {
                return super.getHeaderNames();
            }
            Set<String> result = new LinkedHashSet<>(names);
            result.addAll(Collections.list(super.getHeaderNames()));
            return new Vector<String>(result).elements();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> list = header(name);
            if (list != null) {
                return new Vector<String>(list).elements();
            }
            return super.getHeaders(name);
        }

        @Override
        public String getHeader(String name) {
            List<String> list = header(name);
            if (list != null && !list.isEmpty()) {
                return list.iterator().next();
            }
            return super.getHeader(name);
        }
    }

    protected static class BodyGrabber {
        public Object body(@RequestBody Object body) {
            return body;
        }
    }

    protected static class BodySender {
        @ResponseBody
        public Object body() {
            return null;
        }
    }

}

class ServletOutputToInputConverter extends HttpServletResponseWrapper {

    private StringBuilder builder = new StringBuilder();

    public ServletOutputToInputConverter(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                builder.append(new Character((char) b));
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }

            @Override
            public boolean isReady() {
                return true;
            }
        };
    }

    public ServletInputStream getInputStream() {
        ByteArrayInputStream body = new ByteArrayInputStream(
                builder.toString().getBytes());
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return body.read();
            }

            @Override
            public void setReadListener(ReadListener listener) {
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public boolean isFinished() {
                return body.available() <= 0;
            }
        };
    }

}
