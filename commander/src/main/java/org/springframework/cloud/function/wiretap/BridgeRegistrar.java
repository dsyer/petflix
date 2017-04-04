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

package org.springframework.cloud.function.wiretap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.UnicastProcessor;

/**
 * @author Dave Syer
 *
 */
public class BridgeRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
            BeanDefinitionRegistry registry) {
        AnnotationAttributes attrs = AnnotatedElementUtils.getMergedAnnotationAttributes(
                ClassUtils.resolveClassName(metadata.getClassName(), null),
                EnableBridge.class);
        for (Class<?> type : collectClasses(attrs, metadata.getClassName())) {
            BridgeRegistrar.registerBeanDefinitions(type,
                    StringUtils.uncapitalize(type.getSimpleName()), registry);
        }
    }

    private Class<?>[] collectClasses(AnnotationAttributes attrs, String className) {
        EnableBridge enableBinding = AnnotationUtils.synthesizeAnnotation(attrs,
                EnableBridge.class, ClassUtils.resolveClassName(className, null));
        return enableBinding.value();
    }

    private static void registerBeanDefinitions(Class<?> type, String name,
            BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder consumer = BeanDefinitionBuilder
                .rootBeanDefinition(BridgeProxyFactory.class);
        FluxProcessor<Object, Object> emitter = UnicastProcessor.<Object>create()
                .serialize();
        consumer.addPropertyValue("interfaces", Bridge.class);
        consumer.addConstructorArgValue(emitter);
        RootBeanDefinition bean = (RootBeanDefinition) consumer.getBeanDefinition();
        bean.setTargetType(ResolvableType.forClassWithGenerics(Bridge.class, type));
        registry.registerBeanDefinition(name + "FunctionChannel", bean);
    }

}

@SuppressWarnings("serial")
class BridgeProxyFactory extends ProxyFactoryBean implements MethodInterceptor {

    private final BridgeConsumer consumer;
    private final BridgeSupplier supplier;
    private volatile AtomicBoolean transformed = new AtomicBoolean(false);

    public BridgeProxyFactory(FluxProcessor<Object, Object> emitter) {
        this.consumer = new BridgeConsumer(emitter);
        this.supplier = new BridgeSupplier(emitter);
        addAdvice(this);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("consumer")) {
            return consumer;
        }
        if (invocation.getMethod().getName().equals("supplier")) {
            if (!transformed.getAndSet(true)) {
                Function<Flux<Object>, Flux<Object>> transformer = getTransformer(
                        invocation.getArguments());
                supplier.apply(transformer);
            }
            return supplier;
        }
        return invocation.proceed();

    }

    private Function<Flux<Object>, Flux<Object>> getTransformer(Object[] args) {
        Function<Flux<Object>, Flux<Object>> transformer;
        if (args.length > 0) {
            @SuppressWarnings("unchecked")
            Function<Flux<Object>, Flux<Object>> unchecked = (Function<Flux<Object>, Flux<Object>>) args[0];
            transformer = unchecked;
        }
        else {
            transformer = new BridgeSupplierTransformer();
        }
        return transformer;
    }

}

class BridgeConsumer implements Consumer<Object> {

    private final FluxProcessor<Object, Object> emitter;

    public BridgeConsumer(FluxProcessor<Object, Object> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void accept(Object object) {
        emitter.onNext(object);
    }

}

class BridgeSupplier implements Supplier<Flux<Object>> {

    private Flux<Object> sink;

    public BridgeSupplier(FluxProcessor<Object, Object> emitter) {
        this.sink = emitter;
    }

    public void apply(Function<Flux<Object>, Flux<Object>> transformer) {
        this.sink = transformer.apply(this.sink);
    }

    @Override
    public Flux<Object> get() {
        return sink;
    }

}

class BridgeSupplierTransformer implements Function<Flux<Object>, Flux<Object>> {

    @Override
    public Flux<Object> apply(Flux<Object> flux) {
        return flux.replay().autoConnect();
    }

}
