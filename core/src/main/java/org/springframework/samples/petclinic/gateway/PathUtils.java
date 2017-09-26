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

/**
 * @author Dave Syer
 *
 */
public class PathUtils {

	public static final String PATH_ATTR = "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping";

	public static String forward(String path) {
		if (path.contains("/templates/")) {
			path = path.replace("/templates/", "/");
			if (path.contains(".")) {
				path = path.substring(0, path.indexOf("."));
			}
		}
		else {
			path = "forward:/local" + path;
		}
		return path;
	}

}
