package org.smartrplace.driverhandler.more.bak;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.system.guiappstore.config.GatewayData;

public class GatewaySuperiorPattern extends ResourcePattern<GatewayData> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public GatewaySuperiorPattern(Resource device) {
		super(device);
	}
}
