package org.smartrplace.driverhandler.more;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;

import de.iwes.ogema.bacnet.models.BACnetDevice;

public class BacnetDevicePattern extends ResourcePattern<BACnetDevice> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public BacnetDevicePattern(Resource device) {
		super(device);
	}

	@Override
	public boolean accept() {
		if(!model.getLocation().contains("remoteDevice"))
			return false;
		return true;
	}
}
