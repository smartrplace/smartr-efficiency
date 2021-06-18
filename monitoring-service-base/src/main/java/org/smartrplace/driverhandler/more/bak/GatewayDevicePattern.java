package org.smartrplace.driverhandler.more.bak;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.gateway.device.GatewayDevice;

public class GatewayDevicePattern extends ResourcePattern<GatewayDevice> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public GatewayDevicePattern(Resource device) {
		super(device);
	}
}
