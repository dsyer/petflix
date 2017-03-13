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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * @author Dave Syer
 *
 */
public class ProxyResponseReturnValueHandler implements HandlerMethodReturnValueHandler {

    private RestTemplate template;

    private RequestResponseBodyMethodProcessor delegate;

    public ProxyResponseReturnValueHandler(RestTemplateBuilder builder) {
        template = builder.build();
        delegate = new RequestResponseBodyMethodProcessor(
                template.getMessageConverters());
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        if (!(returnType.getGenericParameterType() instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType type = (ParameterizedType) returnType.getGenericParameterType();
        Type param = type.getActualTypeArguments()[0];
        return type.getRawType().equals(Supplier.class)
                && (param.equals(String.class) || param.equals(URI.class)
                        || param.getTypeName().startsWith(RequestEntity.class.getName()));
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
                    throws Exception {
        @SuppressWarnings("unchecked")
        Supplier<Object> supplier = (Supplier<Object>) returnValue;
        Object request = supplier.get();
        ResponseEntity<List<Object>> response;
        Object body = null;
        if (request instanceof String || request instanceof URI) {
            HeadersBuilder<?> entity;
            URI path = (request instanceof URI) ? (URI) request
                    : new URI((String) request);
            if (returnType.hasMethodAnnotation(GetMapping.class)) {
                entity = (HeadersBuilder<?>) RequestEntity.get(path);
            }
            else if (returnType.hasMethodAnnotation(DeleteMapping.class)) {
                entity = (HeadersBuilder<?>) RequestEntity.delete(path);
            }
            else if (returnType.hasMethodAnnotation(PostMapping.class)) {
                entity = (HeadersBuilder<?>) RequestEntity.post(path);
            }
            else {
                entity = (HeadersBuilder<?>) RequestEntity.put(path);
            }
            boolean accept = false;
            for (Iterator<String> iter = webRequest.getHeaderNames(); iter.hasNext();) {
                String key = iter.next();
                if (key.toLowerCase().equals("accept")) {
                    accept = true;
                }
                entity.header(key, webRequest.getHeaderValues(key));
            }
            if (!accept) {
                entity.header("Accept", MediaType.APPLICATION_JSON_VALUE);
            }
            RequestEntity<?> built = null;
            if (returnType.hasMethodAnnotation(PostMapping.class)
                    || returnType.hasMethodAnnotation(PutMapping.class)) {
                MethodParameter requestBody = getRequestBody(returnType, mavContainer,
                        webRequest);
                if (requestBody != null) {
                    String name = Conventions.getVariableNameForParameter(requestBody);
                    BindingResult result = (BindingResult) mavContainer.getModel()
                            .get(BindingResult.MODEL_KEY_PREFIX + name);
                    body = result.getModel().get(name);
                    built = ((BodyBuilder) entity).body(Arrays.asList(body));
                }
            }
            if (built == null) {
                built = entity.build();
            }
            response = exchange(built);
        }
        else {
            RequestEntity<?> entity = (RequestEntity<?>) request;
            if (!(entity.getBody() instanceof Collection
                    || ObjectUtils.isArray(entity.getBody()))) {
                entity = new RequestEntity<>(Arrays.asList(entity.getBody()), entity.getHeaders(),
                        entity.getMethod(), entity.getUrl(), List.class);
            }
            response = exchange(entity);
        }
        HttpServletResponse servletResponse = webRequest
                .getNativeResponse(HttpServletResponse.class);
        if (servletResponse != null) {
            servletResponse.setStatus(response.getStatusCodeValue());
            for (String key : response.getHeaders().keySet()) {
                servletResponse.setHeader(key, response.getHeaders().getFirst(key));
            }
        }
        if (response.getBody() != null && !response.getBody().isEmpty()) {
            Object result = response.getBody().iterator().next();
            delegate.handleReturnValue(result, returnType, mavContainer, webRequest);
        }
    }

    private ResponseEntity<List<Object>> exchange(RequestEntity<?> entity)
            throws URISyntaxException {
        return template.exchange(entity, new ParameterizedTypeReference<List<Object>>() {
        });
    }

    private MethodParameter getRequestBody(MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
                    throws Exception {
        Method method = returnType.getMethod();
        for (int i = 0; i < method.getParameters().length; i++) {
            MethodParameter param = new MethodParameter(method, i);
            if (param.hasParameterAnnotation(RequestBody.class)) {
                return param;
            }
        }
        MethodParameter input = new MethodParameter(
                ClassUtils.getMethod(getClass(), "body", Map.class), 0);
        delegate.resolveArgument(input, mavContainer, webRequest,
                new DefaultDataBinderFactory(new ConfigurableWebBindingInitializer()));
        return input;
    }

    public Map<String, Object> body(@RequestBody Map<String, Object> body) {
        return body;
    }

}
