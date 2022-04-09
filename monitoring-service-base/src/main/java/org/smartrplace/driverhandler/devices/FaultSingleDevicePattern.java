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

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.actors.Actor;
import org.ogema.model.devices.sensoractordevices.SensorDevice;

import de.iwes.util.resource.ResourceHelper;

public class FaultSingleDevicePattern extends ResourcePattern<Actor> {

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public FaultSingleDevicePattern(Resource device) {
		super(device);
	}
	
	@Override
	public boolean accept() {
		if(model.stateFeedback() instanceof IntegerResource)
			return false;
		SensorDevice sd = ResourceHelper.getFirstParentOfType(model, SensorDevice.class, false);
		if(sd == null) {
			List<Resource> allNodes = model.getReferencingNodes(false);
			for(Resource node: allNodes) {
				SensorDevice sdRef = ResourceHelper.getFirstParentOfType(node, SensorDevice.class, false);
				if((sdRef != null) && DeviceTableRaw.isFaultMessageDevice(sdRef.getLocation(), DeviceTableRaw.getSubResInfo(model)))
					return true;				
			}
			return false;
		}
		if(DeviceTableRaw.isFaultMessageDevice(sd.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return true;
		return false;
	}
}
