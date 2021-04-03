package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.GenericFloatSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.iotawatt.ogema.resources.IotaWattConnection;

public class IotawattSimple_DeviceHandler extends DeviceHandlerSimple<IotaWattConnection> {

	public IotawattSimple_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<IotaWattConnection> getResourceType() {
		return IotaWattConnection.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(IotaWattConnection device,
			InstallAppDevice deviceConfiguration) {
		if(device.sensors().size() == 0)
			return null;
		return device.sensors().getAllElements().get(0).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(IotaWattConnection device,
			InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		//addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		List<GenericFloatSensor> allSens = device.sensors().getAllElements();
		for(GenericFloatSensor ec: allSens) {
			String ch = ec.getName();
			addDatapoint(ec.reading(), result, ch, dpService);
		}
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Iotwatt Meters Base Configuration";
	}

	@Override
	protected Class<? extends ResourcePattern<IotaWattConnection>> getPatternClass() {
		return IotawattSimplePattern.class;
	}

}
