package org.smartrplace.driverhandler.more;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;

import de.smartrplace.ghl.ogema.resources.GhlWaterPond;

public class GhlWaterPondPattern extends ResourcePattern<GhlWaterPond> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public GhlWaterPondPattern(Resource device) {
		super(device);
	}

}
