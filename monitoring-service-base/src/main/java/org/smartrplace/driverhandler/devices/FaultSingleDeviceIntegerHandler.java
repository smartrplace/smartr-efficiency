package org.smartrplace.driverhandler.devices;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.actors.Actor;

public class FaultSingleDeviceIntegerHandler extends FaultSingleDeviceHandler {

	public FaultSingleDeviceIntegerHandler(ApplicationManagerPlus appMan) {
		super(appMan);
	}

	@Override
	public String getTableTitle() {
		return "Fault Single Devices (Integer State)";
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "FAUI";
	}
	
	@Override
	protected Class<? extends ResourcePattern<Actor>> getPatternClass() {
		return FaultSingleDeviceIntegerPattern.class;
	}
	
	@Override
	public boolean addDeviceOrResourceListToSync() {
		return false;
	}
}
