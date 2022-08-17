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
import org.ogema.model.connections.ThermalMixingConnection;
import org.ogema.model.sensors.GenericBinarySensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ThermalMixingCirc_DeviceHandler extends DeviceHandlerSimple<ThermalMixingConnection> {

	public ThermalMixingCirc_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<ThermalMixingConnection> getResourceType() {
		return ThermalMixingConnection.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ThermalMixingConnection device, InstallAppDevice deviceConfiguration) {
		return device.outputTemperature().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ThermalMixingConnection device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.returnTemperature().reading(), result);
		addDatapoint(device.pump().onOffSwitch().stateControl(), result);
		addDatapoint(device.pump().onOffSwitch().stateFeedback(), result);
		addDatapoint(device.pump().getSubResource("malfunction", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.valve().setting().stateControl(), result);
		addDatapoint(device.valve().setting().stateFeedback(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Mixing Connections and PreHeatings BACnet";
	}

	@Override
	protected Class<? extends ResourcePattern<ThermalMixingConnection>> getPatternClass() {
		return ThermalMixingCirc_Pattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "THMC";
	}
}
