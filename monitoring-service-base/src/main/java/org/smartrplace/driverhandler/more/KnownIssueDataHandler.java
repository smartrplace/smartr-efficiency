package org.smartrplace.driverhandler.more;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.KnownIssueDataGw;

public class KnownIssueDataHandler extends DeviceHandlerSimple<KnownIssueDataGw> {

	public KnownIssueDataHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<KnownIssueDataGw> getResourceType() {
		return KnownIssueDataGw.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(KnownIssueDataGw device, InstallAppDevice deviceConfiguration) {
		return device.activeAlarmSupervision();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(KnownIssueDataGw device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		MemoryTsPSTHandler.addDatapointForcedPST(device.activeAlarmSupervision(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.datapointsInAlarmState(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.devicesTotal(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.datapointsTotal(), result, dpService);
		
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesUnassigned(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesAssignedOther(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesAssignedOperationOwn(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesAssignedDevOwn(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesAssignedCustomer(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesOpExternal(), result, dpService);
		MemoryTsPSTHandler.addDatapointForcedPST(device.knownIssuesDevExternal(), result, dpService);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Known Issue Management";
	}

	@Override
	protected Class<? extends ResourcePattern<KnownIssueDataGw>> getPatternClass() {
		return KnownIssueDataPattern.class;
	}

}
