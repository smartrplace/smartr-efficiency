package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.metering.GasMeter;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess.StandardDeviceEval;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class HMGas_MeterDeviceHandler extends DeviceHandlerSimple<GasMeter> {

	public HMGas_MeterDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<GasMeter> getResourceType() {
		return GasMeter.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(GasMeter device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(GasMeter device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();

		Datapoint dp = addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		
		if(Boolean.getBoolean("virtualSensors.disable")) {
			return result;
		}
		if(dp == null) {
			return result;
		}
		
		dp.info().setAggregationMode(AggregationMode.Meter2Meter);
		Datapoint evalDp = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
				StandardDeviceEval.COUNTER_TO_15MIN, dpService);
		result.add(evalDp);
		Datapoint evalDpDaily = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
				StandardDeviceEval.COUNTER_TO_DAILY_B15, dpService);
		if(!Boolean.getBoolean("meterConfigs.createResourceForAlarming"))
			result.add(evalDpDaily);
		else {
			FloatResource dailyTraffic = device.getSubResource("energySumDaily", FloatResource.class);
			dailyTraffic.create().activate(true);
			Datapoint dpDaily = StandardEvalAccess.addVirtualDatapoint(dailyTraffic,
				evalDpDaily, dpService, result);
			dpDaily.addToSubRoomLocationAtomic(null, null, device.getName()+"-daily", false);
		}

		return result;
	}

	@Override
	public String getTableTitle() {
		return "Gas Meters";
	}

	@Override
	protected Class<? extends ResourcePattern<GasMeter>> getPatternClass() {
		return HMGas_MeterPattern.class;
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "GAS";
	}

}
