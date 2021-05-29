package org.smartrplace.driverhandler.more;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.gateway.device.KnownIssueDataGw;

public class KnownIssueDataPattern extends ResourcePattern<KnownIssueDataGw> { 
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public KnownIssueDataPattern(Resource device) {
		super(device);
	}
}
