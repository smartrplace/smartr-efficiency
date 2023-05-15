package org.smartrplace.apps.alarmingconfig.writeabledps;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.spapi.model.WriteableDatapoint;

public class WriteableDatapointPattern extends ResourcePattern<WriteableDatapoint> { 
	
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
	public WriteableDatapointPattern(Resource device) {
		super(device);
	}

}
