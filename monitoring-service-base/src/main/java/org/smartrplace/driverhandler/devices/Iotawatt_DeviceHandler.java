package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection;

public class Iotawatt_DeviceHandler extends DeviceHandlerSimple<IotaWattElectricityConnection> {

	public Iotawatt_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<IotaWattElectricityConnection> getResourceType() {
		return IotaWattElectricityConnection.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(IotaWattElectricityConnection device,
			InstallAppDevice deviceConfiguration) {
		return device.elConn().voltageSensor().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(IotaWattElectricityConnection device,
			InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		for(ElectricityConnection ec: device.elConn().subPhaseConnections().getAllElements()) {
			String ph = ec.getName();
			addDatapoint(ec.energySensor().reading(), result, ph, dpService);
			addDatapoint(ec.powerSensor().reading(), result, ph, dpService);
		}
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "Iotwatt Meters";
	}

	@Override
	protected Class<? extends ResourcePattern<IotaWattElectricityConnection>> getPatternClass() {
		return IotawattPattern.class;
	}

}
