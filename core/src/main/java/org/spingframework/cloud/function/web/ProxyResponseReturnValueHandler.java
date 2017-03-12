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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    ProxyResponseReturnValueHandler(RestTemplateBuilder builder) {
        template = builder.build();
        delegate = new RequestResponseBodyMethodProcessor(
                template.getMessageConverters());
    }

    private Map<String, Object> post(Object body, String path) throws URISyntaxException {
        BodyBuilder request = RequestEntity.post(new URI(path))
                .contentType(MediaType.APPLICATION_JSON);
        return exchange(request, body);
    }

    private Map<String, Object> put(Object body, String path) throws URISyntaxException {
        BodyBuilder request = RequestEntity.put(new URI(path))
                .contentType(MediaType.APPLICATION_JSON);
        return exchange(request, body);
    }

    private Map<String, Object> delete(String path) throws URISyntaxException {
        return template.exchange(
                RequestEntity.delete(new URI(path)).accept(MediaType.APPLICATION_JSON)
                        .build(),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody().iterator().next();
    }

    private Map<String, Object> exchange(BodyBuilder request, Object body)
            throws URISyntaxException {
        return template
                .exchange(
                        request.accept(MediaType.APPLICATION_JSON)
                                .body(Arrays.asList(body)),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        })
                .getBody().iterator().next();
    }

    private Map<String, Object> get(String path) throws URISyntaxException {
        return template.exchange(
                RequestEntity.get(new URI(path)).accept(MediaType.APPLICATION_JSON)
                        .build(),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody().iterator().next();
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        if (!(returnType.getGenericParameterType() instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType type = (ParameterizedType) returnType.getGenericParameterType();
        return type.getRawType().equals(Supplier.class)
                && type.getActualTypeArguments()[0].equals(String.class);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
                    throws Exception {
        @SuppressWarnings("unchecked")
        Supplier<String> supplier = (Supplier<String>) returnValue;
        String path = supplier.get();
        Map<String, Object> map;
        if (returnType.hasMethodAnnotation(GetMapping.class)) {
            map = get(path);
        }
        else if (returnType.hasMethodAnnotation(DeleteMapping.class)) {
            map = delete(path);
        }
        else {
            Object body = null;
            MethodParameter requestBody = getRequestBody(returnType);
            if (requestBody != null) {
                String name = Conventions.getVariableNameForParameter(requestBody);
                BindingResult result = (BindingResult) mavContainer.getModel()
                        .get(BindingResult.MODEL_KEY_PREFIX + name);
                body = result.getModel().get(name);
            }
            if (returnType.hasMethodAnnotation(DeleteMapping.class)) {
                map = post(body, path);
            }
            else {
                map = put(body, path);
            }

        }
        delegate.handleReturnValue(map, returnType, mavContainer, webRequest);
    }

    private MethodParameter getRequestBody(MethodParameter returnType) {
        Method method = returnType.getMethod();
        for (int i = 0; i < method.getParameters().length; i++) {
            MethodParameter param = new MethodParameter(method, i);
            if (param.hasParameterAnnotation(RequestBody.class)) {
                return param;
            }
        }
        return returnType;
    }

}
