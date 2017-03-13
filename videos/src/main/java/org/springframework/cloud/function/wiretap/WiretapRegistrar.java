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

import java.time.Duration;
import java.util.function.Consumer;
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
public class WiretapRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
            BeanDefinitionRegistry registry) {
        AnnotationAttributes attrs = AnnotatedElementUtils.getMergedAnnotationAttributes(
                ClassUtils.resolveClassName(metadata.getClassName(), null),
                EnableWiretap.class);
        for (Class<?> type : collectClasses(attrs, metadata.getClassName())) {
            WiretapRegistrar.registerBeanDefinitions(type,
                    StringUtils.uncapitalize(type.getSimpleName()), registry);
        }
    }

    private Class<?>[] collectClasses(AnnotationAttributes attrs, String className) {
        EnableWiretap enableBinding = AnnotationUtils.synthesizeAnnotation(attrs,
                EnableWiretap.class, ClassUtils.resolveClassName(className, null));
        return enableBinding.value();
    }

    private static void registerBeanDefinitions(Class<?> type, String name,
            BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder consumer = BeanDefinitionBuilder
                .rootBeanDefinition(ConsumerProxyFactory.class);
        FluxProcessor<Object, Object> emitter = UnicastProcessor.<Object>create()
                .serialize();
        consumer.addPropertyValue("interfaces", Consumer.class);
        consumer.addConstructorArgValue(emitter);
        RootBeanDefinition bean = (RootBeanDefinition) consumer.getBeanDefinition();
        bean.setTargetType(ResolvableType.forClassWithGenerics(Consumer.class, type));
        registry.registerBeanDefinition(name + "Consumer", bean);
        BeanDefinitionBuilder supplier = BeanDefinitionBuilder
                .rootBeanDefinition(SupplierProxyFactory.class);
        supplier.addPropertyValue("interfaces", Supplier.class);
        supplier.addConstructorArgValue(emitter);
        RootBeanDefinition output = (RootBeanDefinition) supplier.getBeanDefinition();
        output.setTargetType(ResolvableType.forClassWithGenerics(Supplier.class,
                ResolvableType.forClassWithGenerics(Flux.class, type)));
        registry.registerBeanDefinition(name + "Supplier", output);
    }

}

class ConsumerProxyFactory extends ProxyFactoryBean implements MethodInterceptor {

    private final FluxProcessor<Object, Object> emitter;

    public ConsumerProxyFactory(FluxProcessor<Object, Object> emitter) {
        this.emitter = emitter;
        addAdvice(this);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("accept")) {
            emitter.onNext(invocation.getArguments()[0]);
            return null;
        }
        return invocation.proceed();
    }

}

class SupplierProxyFactory extends ProxyFactoryBean implements MethodInterceptor {

    private final Flux<Object> sink;

    public SupplierProxyFactory(FluxProcessor<Object, Object> emitter) {
        // TODO: maybe think about the contract for clients a bit more. Right now we allow
        // successive clients to receive duplicate data if they subscribe within 100ms of
        // each other (c.f. the timeout for closing the client connection is 1000ms).
        sink = emitter.log().replay(Duration.ofMillis(100L)).autoConnect().log();
        addAdvice(this);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("get")) {
            return sink;
        }
        return invocation.proceed();
    }

}
