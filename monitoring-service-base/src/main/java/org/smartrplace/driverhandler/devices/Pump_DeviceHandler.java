package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.buildingtechnology.Pump;
import org.ogema.model.sensors.GenericBinarySensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class Pump_DeviceHandler extends DeviceHandlerSimple<Pump> {

	public Pump_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<Pump> getResourceType() {
		return Pump.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(Pump device, InstallAppDevice deviceConfiguration) {
		return device.onOffSwitch().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(Pump device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.onOffSwitch().stateControl(), result);
		addDatapoint(device.getSubResource("malfunction", GenericBinarySensor.class).reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Pumps BACnet";
	}

	@Override
	protected Class<? extends ResourcePattern<Pump>> getPatternClass() {
		return Pump_Pattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "PMP";
	}
}
