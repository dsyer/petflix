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
        registry.registerBeanDefinition(name + "Bridge", bean);
    }

}

@SuppressWarnings("serial")
class BridgeProxyFactory extends ProxyFactoryBean implements MethodInterceptor {

    private final DefaultBridge<Object> delegate;

    public BridgeProxyFactory(FluxProcessor<Object, Object> emitter) {
        this.delegate = new DefaultBridge<Object>(emitter);
        addAdvice(this);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("consumer")
                || invocation.getMethod().getName().equals("supplier")) {
            return invocation.getMethod().invoke(delegate, invocation.getArguments());
        }
        return invocation.proceed();
    }

}

