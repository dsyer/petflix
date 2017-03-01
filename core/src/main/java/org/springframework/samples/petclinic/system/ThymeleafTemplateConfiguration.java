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

package org.springframework.samples.petclinic.system;

import java.net.URI;
import java.util.Map;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Dave Syer
 *
 */
@Configuration
public class ThymeleafTemplateConfiguration {

    @Bean
    public ITemplateResolver remoteTemplateResolver(
            @Value("${services.video.uri}") URI videosUrl) {
        UrlTemplateResolver resolver = new UrlTemplateResolver() {
            @Override
            protected ITemplateResource computeTemplateResource(
                    IEngineConfiguration configuration, String ownerTemplate,
                    String template, String resourceName, String characterEncoding,
                    Map<String, Object> templateResolutionAttributes) {
                if (!resourceName.startsWith("http:")
                        && !resourceName.startsWith("https:")) {
                    return null;
                }
                UriComponents videos = UriComponentsBuilder.fromUri(videosUrl).build();
                resourceName = UriComponentsBuilder.fromHttpUrl(resourceName)
                        .host(videos.getHost()).scheme(videos.getScheme())
                        .port(videos.getPort()).build().toString();
                return super.computeTemplateResource(configuration, ownerTemplate,
                        template, resourceName, characterEncoding,
                        templateResolutionAttributes);
            }
        };
        resolver.setCacheable(false); // devtime only?
        return resolver;
    }

}
