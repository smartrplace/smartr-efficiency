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
import org.ogema.model.connections.ThermalMixingConnection;
import org.ogema.model.devices.buildingtechnology.Pump;

public class Pump_Pattern extends ResourcePattern<Pump> {

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public Pump_Pattern(Resource device) {
		super(device);
	}
	
	@Override
	public boolean accept() {
		Resource parent = model.getParent();
		if(parent != null && parent instanceof ThermalMixingConnection)
			return false;
		//if(DeviceTableRaw.isHeatMeterDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
		return true;
		//return false;
	}
}
