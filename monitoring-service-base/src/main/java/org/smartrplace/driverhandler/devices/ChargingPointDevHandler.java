package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.storage.ChargingPoint;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ChargingPointDevHandler extends DeviceHandlerSimple<ChargingPoint> {
	
	public ChargingPointDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}
	
	@Override
	public Class<ChargingPoint> getResourceType() {
		return ChargingPoint.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		if (device.electricityConnection().powerSensor().reading().isActive()) {
			return device.electricityConnection().powerSensor().reading();
		}
		return device.setting().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.setting().stateControl(), result);
		addDatapoint(device.setting().stateFeedback(), result);
		addDatapoint(device.battery().chargeSensor().reading(), result);
		
		addDatapoint(device.electricityConnection().powerSensor().reading(), "Actual Power", result);
		Datapoint energyDp = addDatapoint(device.electricityConnection().energySensor().reading(), "Total Energy", result);
		Resource isPlugged = device.getSubResource("isPlugged");
		if (isPlugged != null && isPlugged instanceof IntegerResource) {
			addDatapoint((IntegerResource) isPlugged, "Plugged", result);
		}
		Resource isCharging = device.getSubResource("isCharging");
		if (isCharging != null && isCharging instanceof IntegerResource) {
			addDatapoint((IntegerResource) isCharging, "Charging", result);
		}
		
		if(energyDp != null) {
			MeteringEvalUtil.addDailyMeteringEval(energyDp, null, device.electricityConnection(), result, appMan);			
		}

		return result;
	}

	@Override
	public String getTableTitle() {
		return "Charging Stations";
	}

	@Override
	protected Class<? extends ResourcePattern<ChargingPoint>> getPatternClass() {
		return ChargingPointPattern.class;
	}

	@Override
	public ComType getComType() {
		return ComType.IP;
	}
	
}
