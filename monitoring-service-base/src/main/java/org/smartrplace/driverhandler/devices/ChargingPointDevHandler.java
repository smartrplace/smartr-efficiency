package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.storage.ChargingPoint;
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
	protected SingleValueResource getMainSensorValue(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		return device.setting().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.setting().stateControl(), result);
		addDatapoint(device.setting().stateFeedback(), result);
		addDatapoint(device.battery().chargeSensor().reading(), result);
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "Charging Stations";
	}

	@Override
	protected Class<? extends ResourcePattern<ChargingPoint>> getPatternClass() {
		return ChargingPointPattern.class;
	}

}
