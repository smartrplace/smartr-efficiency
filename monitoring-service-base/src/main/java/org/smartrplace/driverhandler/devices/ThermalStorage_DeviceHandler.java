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
import org.ogema.model.connections.ThermalConnection;
import org.ogema.model.devices.storage.ThermalStorage;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.StateOfChargeSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ThermalStorage_DeviceHandler extends DeviceHandlerSimple<ThermalStorage> {

	public ThermalStorage_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<ThermalStorage> getResourceType() {
		return ThermalStorage.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ThermalStorage device, InstallAppDevice deviceConfiguration) {
		if(device.chargeSensor().reading().isActive())
			return device.chargeSensor().reading();
		if(device.storageTemperature().reading().isActive())
			return device.storageTemperature().reading();
		return device.getSubResource("storageTemperatureMiddle", TemperatureSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ThermalStorage device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		SingleValueResource main = getMainSensorValue(device, deviceConfiguration);
		addDatapoint(main, result);
		addDatapoint(device.getSubResource("storageTemperatureBottom", TemperatureSensor.class).reading(), result);
		if(!main.getLocation().contains("storageTemperatureMiddle"))
			addDatapoint(device.getSubResource("storageTemperatureMiddle", TemperatureSensor.class).reading(), result);
		addDatapoint(device.getSubResource("storageTemperatureTop", TemperatureSensor.class).reading(), result);
		if(!main.getLocation().contains("/storageTemperature/"))
			addDatapoint(device.storageTemperature().reading(), result);
		addDatapoint(device.getSubResource("icingDegree", GenericFloatSensor.class).reading(), result);
		addDatapoint(device.getSubResource("fillLevel", GenericFloatSensor.class).reading(), result);
		addDatapoint(device.heatConnections().getSubResource("heatExchangerExtraction", ThermalConnection.class).inputTemperature().reading(), result);
		addDatapoint(device.heatConnections().getSubResource("heatExchangerExtraction", ThermalConnection.class).outputTemperature().reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Thermal Storages";
	}

	@Override
	protected Class<? extends ResourcePattern<ThermalStorage>> getPatternClass() {
		return ThermalStorage_Pattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "THST";
	}
}
