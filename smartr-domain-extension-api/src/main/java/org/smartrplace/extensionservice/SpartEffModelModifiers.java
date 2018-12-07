/**
 * Copyright 2011-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.extensionservice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;

/** List of annotations used to describe the meaning and behavior of elements of resource types */
public class SpartEffModelModifiers {
	/**
	 * Only relevant to {@link SmartEffTimeSeries}
	 */
	@Target(ElementType.METHOD)
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DataType {
		Class<? extends SingleValueResource> resourcetype() default FloatResource.class;
	}
}
