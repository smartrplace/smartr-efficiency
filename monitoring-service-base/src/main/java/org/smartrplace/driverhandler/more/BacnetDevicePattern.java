package org.smartrplace.driverhandler.more;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;

import de.iwes.ogema.bacnet.models.BACnetDevice;
import de.smartrplace.ghl.ogema.resources.GhlWaterPond;

public class BacnetDevicePattern extends ResourcePattern<BACnetDevice> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public BacnetDevicePattern(Resource device) {
		super(device);
	}

}
