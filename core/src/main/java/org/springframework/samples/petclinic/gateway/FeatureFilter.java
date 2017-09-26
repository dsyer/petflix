/*
 * Copyright 2016-2017 the original author or authors.
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * @author Dave Syer
 *
 */
@Component
@ConfigurationProperties("feature")
public class FeatureFilter extends CookieGenerator implements Filter {

    public static final String PETFLIX_ENABLED = "petflix-enabled";
    private double rate = 0.5;
    
    public FeatureFilter() {
        setCookieName(PETFLIX_ENABLED);
    }

    public double getRate() {
        return this.rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        Cookie cookie = WebUtils.getCookie((HttpServletRequest) request, getCookieName());
        String value;
        if (cookie == null) {
            value = Math.random() < rate ? "true" : "false";
            addCookie((HttpServletResponse) response, value);
        } else {
            value = cookie.getValue();
        }
        request.setAttribute(PETFLIX_ENABLED, value);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
