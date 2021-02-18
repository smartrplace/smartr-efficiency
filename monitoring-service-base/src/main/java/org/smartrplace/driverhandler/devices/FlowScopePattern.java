package org.smartrplace.driverhandler.devices;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.metering.special.FlowProbe;

public class FlowScopePattern extends ResourcePattern<FlowProbe> {

	public FlowScopePattern(Resource match) {
		super(match);
	}

}
