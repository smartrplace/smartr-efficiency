package org.smartrplace.driverhandler.more;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;

public class MemoryTsPSTPattern extends ResourcePattern<MemoryTimeseriesPST> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public MemoryTsPSTPattern(Resource device) {
		super(device);
	}
}
