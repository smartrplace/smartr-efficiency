/**
 * ﻿Copyright 2018 Smartrplace UG
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
package org.smartrplace.driverhandler.devices;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.connections.ThermalConnection;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.TemperatureSensor;

import de.iwes.util.resource.ResourceHelper;

public class HeatingLabTempsSensPattern extends ResourcePattern<TemperatureSensor> {

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public HeatingLabTempsSensPattern(Resource device) {
		super(device);
	}
	
	@Override
	public boolean accept() {
		if(model.getLocation().startsWith("HeatingLabData")) {
			Resource parent = model.getParent();
			if(parent == null || parent.getResourceType().equals(ThermalConnection.class))
				return false;
			return true;
		}
		if(ResourceHelper.hasParentAboveType(model, PhysicalElement.class) < 0)
			return true;
		return false;
	}
}
