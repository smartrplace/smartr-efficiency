package org.smartrplace.homematic.devicetable;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.sensoractordevices.SensorDevice;

public class TemperatureOrHumiditySensorPattern extends ResourcePattern<SensorDevice> { 
	
	/**
	 * Device name. Only devices whose "name"
	 * subresource is active will be reported as completed patterns.
	 */
	//@Access(mode = AccessMode.READ_ONLY)
	//@Existence(required=CreateMode.OPTIONAL)
	//public StringResource name = model.name();

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public TemperatureOrHumiditySensorPattern(Resource device) {
		super(device);
	}

	@Override
	public boolean accept() {
		return DeviceTableRaw.isTempHumSens(model);
	}
}
