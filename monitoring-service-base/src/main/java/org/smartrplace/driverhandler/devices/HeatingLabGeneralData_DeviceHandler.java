package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.fhg.iee.ogema.labdata.HeatingLabData;

public class HeatingLabGeneralData_DeviceHandler extends DeviceHandlerSimple<HeatingLabData> {

	public HeatingLabGeneralData_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<HeatingLabData> getResourceType() {
		return HeatingLabData.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(HeatingLabData device, InstallAppDevice deviceConfiguration) {
		return device.mainConnection().inputTemperature().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(HeatingLabData device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.mainConnection().outputTemperature().reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Heating Lab Central Connection Data";
	}

	@Override
	protected Class<? extends ResourcePattern<HeatingLabData>> getPatternClass() {
		return HeatingLabGeneralDataPattern.class;
	}

}
