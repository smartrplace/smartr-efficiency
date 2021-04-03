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

import de.smartrplace.ble.resources.BeaconInformation;

public class BluetoothBeaconHandler extends DeviceHandlerSimple<BeaconInformation> {

	public BluetoothBeaconHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<BeaconInformation> getResourceType() {
		return BeaconInformation.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(BeaconInformation device, InstallAppDevice deviceConfiguration) {
		return device.rssiSensor().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(BeaconInformation device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Bluetooth Beacons";
	}

	@Override
	protected Class<? extends ResourcePattern<BeaconInformation>> getPatternClass() {
		return BluetoothBeaconPattern.class;
	}

}
