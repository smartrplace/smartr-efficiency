package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess.StandardDeviceEval;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class GasEnergyCam_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public GasEnergyCam_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("VOLUME_0_0", VolumeAccumulatedSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();

		// We have to sychronize with reading remote slotsdb and setting up the time series for mirror devices here
		Resource mirrorList = appMan.getResourceAccess().getResource("serverMirror");
		if(mirrorList != null) {
			IntegerResource initStatus = mirrorList.getSubResource("initStatus", IntegerResource.class);
			while(initStatus.isActive() && (initStatus.getValue() < 2) && Boolean.getBoolean("org.smartrplace.app.srcmon.iscollectinggateway")) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		Datapoint dp = addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		
		if(Boolean.getBoolean("virtualSensors.disable")) {
			//System.out.println("   *** Disabling HM_IEC virtualSensors based on energySensor: "+device.getLocation());
			return result;
		}
		if(dp == null) {
			//System.out.println("   !!! WARNING: HM-IEC without energySensor to use: "+device.getLocation());
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
		return "Gas Energy Cams";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return GasEnergyCam_SensorDevicePattern.class;
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "GEC";
	}

}
