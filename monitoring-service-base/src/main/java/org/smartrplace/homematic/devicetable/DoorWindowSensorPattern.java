package org.smartrplace.homematic.devicetable;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.sensors.DoorWindowSensor;

public class DoorWindowSensorPattern extends ResourcePattern<DoorWindowSensor> { 
	
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
	public DoorWindowSensorPattern(Resource device) {
		super(device);
	}

}
