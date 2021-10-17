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
package org.smartrplace.homematic.devicetable;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.prototypes.PhysicalElement;

import de.iwes.util.resource.ResourceHelper;


public class OnOffSwitchPattern extends ResourcePattern<OnOffSwitch> {

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public OnOffSwitchPattern(Resource device) {
		super(device);
	}

	@Override
	public boolean accept() {
		Resource parent = model.getParent();
		if(model.location().room().isActive())
			return true;
		if(model.getName().startsWith("VIRTUAL_SWITCH_FEEDBACK_"))
			return false;
		if(DeviceTableRaw.isCO2SensorHm(model.getLocation()))
			return false;
		if(parent == null || parent.getResourceType().getSimpleName().equals("HmDevice"))
			return true;
		if(model.getName().toLowerCase().startsWith("installationmode"))
			return false;
		if(ResourceHelper.hasParentAboveType(model, PhysicalElement.class) < 0)
			return true;
		return false;
	}
}
