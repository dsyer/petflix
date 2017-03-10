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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.util.Assert;
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
        Set<String> basePackages = getPackagesToScan(metadata);
        ClassPathScanningCandidateComponentProvider scanner = getClassPathScanner();
        for (String pkg : basePackages) {
            Set<BeanDefinition> components = scanner.findCandidateComponents(pkg);
            for (BeanDefinition component : components) {
                Class<?> type = ClassUtils.resolveClassName(component.getBeanClassName(),
                        null);
                WiretapRegistrar.registerBeanDefinitions(type,
                        StringUtils.uncapitalize(type.getSimpleName()), registry);
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getClassPathScanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false) {
            protected boolean isCandidateComponent(
                    AnnotatedBeanDefinition beanDefinition) {
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                return metadata.isIndependent() && metadata.isInterface();
            }
        };
        scanner.addIncludeFilter(new AbstractTypeHierarchyTraversingFilter(true, true) {
            @Override
            protected Boolean matchInterface(String interfaceName) {
                return interfaceName.equals(Consumer.class.getName());
            }
        });
        return scanner;
    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(metadata.getAnnotationAttributes(WiretapScan.class.getName()));
        String[] basePackages = attributes.getStringArray("basePackages");
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        Set<String> packagesToScan = new LinkedHashSet<String>();
        packagesToScan.addAll(Arrays.asList(basePackages));
        for (Class<?> basePackageClass : basePackageClasses) {
            packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
        }
        if (packagesToScan.isEmpty()) {
            String packageName = ClassUtils.getPackageName(metadata.getClassName());
            Assert.state(!StringUtils.isEmpty(packageName),
                    "@WiretapScan cannot be used with the default package");
            return Collections.singleton(packageName);
        }
        return packagesToScan;
    }

    private static void registerBeanDefinitions(Class<?> type, String name,
            BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder consumer = BeanDefinitionBuilder
                .genericBeanDefinition(ConsumerProxyFactory.class);
        FluxProcessor<Object, Object> emitter = UnicastProcessor.<Object>create()
                .serialize();
        consumer.addPropertyValue("interfaces", type);
        consumer.addConstructorArgValue(emitter);
        registry.registerBeanDefinition(name, consumer.getBeanDefinition());
        BeanDefinitionBuilder supplier = BeanDefinitionBuilder
                .genericBeanDefinition(SupplierProxyFactory.class);
        supplier.addPropertyValue("interfaces", Supplier.class);
        supplier.addConstructorArgValue(emitter);
        registry.registerBeanDefinition(name + "Supplier", supplier.getBeanDefinition());
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
        sink = emitter.publish().autoConnect();
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
