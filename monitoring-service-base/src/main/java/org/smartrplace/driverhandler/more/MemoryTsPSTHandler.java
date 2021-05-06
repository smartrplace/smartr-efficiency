package org.smartrplace.driverhandler.more;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;

public class MemoryTsPSTHandler extends DeviceHandlerSimple<MemoryTimeseriesPST> {

	public MemoryTsPSTHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<MemoryTimeseriesPST> getResourceType() {
		return MemoryTimeseriesPST.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(MemoryTimeseriesPST device, InstallAppDevice deviceConfiguration) {
		return device.pstBlockingCounter();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(MemoryTimeseriesPST device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapointForcedPST(device.pstMultiToSingleEvents(), result, dpService);
		addDatapointForcedPST(device.pstMultiToSingleCounter(), result, dpService);
		addDatapointForcedPST(device.pstMultiToSingleAggregations(), result, dpService);
		addDatapointForcedPST(device.pstMultiToSingleAggregationsCounter(), result, dpService);

		addDatapointForcedPST(device.pstBlockingSingeEvents(), result, dpService);
		addDatapointForcedPST(device.pstBlockingCounter(), result, dpService);
		addDatapointForcedPST(device.pstSubTsBuild(), result, dpService);
		addDatapointForcedPST(device.pstSubTsBuildCounter(), result, dpService);

		addDatapointForcedPST(device.pstUpdateValuesPS2(), result, dpService);
		addDatapointForcedPST(device.pstUpdateValuesPS2Counter(), result, dpService);
		addDatapointForcedPST(device.pstTSServlet(), result, dpService);
		addDatapointForcedPST(device.pstTSServletCounter(), result, dpService);
		return result;
	}

	protected Datapoint addDatapointForcedPST(SingleValueResource sres, List<Datapoint> result,
			DatapointService dpService) {
		String subPst = sres.getName();
		if(subPst.startsWith("pst"))
			subPst = subPst.substring("pst".length());
		return addDatapointForced(sres, result, subPst, dpService);
	}
	protected Datapoint addDatapointForced(SingleValueResource sres, List<Datapoint> result,
			String subLocation, DatapointService dpService) {
		if(!sres.exists()) {
			sres.create();
			sres.activate(false);
		}
		Datapoint dp = dpService.getDataPointStandard(sres);
		if(dp != null && subLocation != null) {
			dp.addToSubRoomLocationAtomic(null, null, subLocation, false);
		}
		result.add(dp);
		return dp;
	}

	@Override
	public String getTableTitle() {
		return "Memory Timeseries Supervison";
	}

	@Override
	protected Class<? extends ResourcePattern<MemoryTimeseriesPST>> getPatternClass() {
		return MemoryTsPSTPattern.class;
	}

}
