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
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.sensoractordevices.SensorDevice;

public class SensorDeviceGenericPattern extends ResourcePattern<SensorDevice> {

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public SensorDeviceGenericPattern(Resource device) {
		super(device);
	}
	
	@Override
	public boolean accept() {
		if(DeviceTableRaw.isWeatherStation(model.getLocation()))
			return false;
		if(DeviceTableRaw.isOpenWeatherMapSensorDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isTempHumSens(model.getLocation()))
			return false;
		if(DeviceTableRaw.isFALorFALMOT(model.getLocation()))
			return false;
		if(DeviceTableRaw.isDimmerSensorDevice(model.getLocation()))
			return false;
		if(DeviceTableRaw.isSmartProtectDevice(model.getLocation()))
			return false;
		if(DeviceTableRaw.isWaterMeterDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isGasEnergyCamDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isHeatCostAllocatorDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isWiredMBusMasterDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isHAPDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		if(DeviceTableRaw.isFaultMessageDevice(model.getLocation(), DeviceTableRaw.getSubResInfo(model)))
			return false;
		//If more special SensorDevices are supported in the future add check here
		return true;
		/*if(model.getLocation().startsWith("JMBUS_BASE"))
			return true;
		for(Sensor sens: model.getSubResources(Sensor.class, false)) {
			if(sens instanceof TemperatureSensor || sens instanceof HumiditySensor)
				continue;
			return true;
		}
		return false;*/
	}
}
