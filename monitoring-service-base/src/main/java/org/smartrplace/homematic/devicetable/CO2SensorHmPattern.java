package org.smartrplace.homematic.devicetable;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.sensors.CO2Sensor;

public class CO2SensorHmPattern extends ResourcePattern<CO2Sensor> { 
	
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
	public CO2SensorHmPattern(Resource device) {
		super(device);
	}

	@Override
	public boolean accept() {
		if(model.getLocation().startsWith("knx"))
			return true;
		return DeviceTableRaw.isCO2SensorHm(model.getLocation());
	}
}
