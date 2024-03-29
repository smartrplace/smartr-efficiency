package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.connections.ThermalConnection;
import org.ogema.model.devices.storage.ThermalStorage;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
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

	Set<String> initActivate = new HashSet<>(); 
	@Override
	public SingleValueResource getMainSensorValue(ThermalStorage device, InstallAppDevice deviceConfiguration) {
		if(!initActivate.contains(device.getLocation())) {
			initActivate.add(device.getLocation());
			ThermalStorage dev2 = device.getLocationResource();
			dev2.activate(true);
		}
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
		String mainLoc = main.getLocation();
		if(!mainLoc.contains("storageTemperatureMiddle")) {
			addDatapoint(main, result);
		}
		addDatapointWithResOrSensorName(device.getSubResource("storageTemperatureMiddle", TemperatureSensor.class).reading(), result);
		addDatapointWithResOrSensorName(device.getSubResource("storageTemperatureBottom", TemperatureSensor.class).reading(), result);
		addDatapointWithResOrSensorName(device.getSubResource("storageTemperatureTop", TemperatureSensor.class).reading(), result);
		if(!main.getLocation().contains("/storageTemperature/"))
			addDatapoint(device.storageTemperature().reading(), result);
		addDatapointWithResOrSensorName(device.getSubResource("icingDegree", GenericFloatSensor.class).reading(), result);
		addDatapointWithResOrSensorName(device.getSubResource("fillLevel", GenericFloatSensor.class).reading(), result);
		addDatapoint(device.heatConnections().getSubResource("heatExchangerExtraction", ThermalConnection.class).inputTemperature().reading(), result);
		addDatapoint(device.heatConnections().getSubResource("heatExchangerExtraction", ThermalConnection.class).outputTemperature().reading(), result);
		
		addEnergyHeatMeter(device, "heatExchangerExtraction", result);
		addEnergyHeatMeter(device, "absorber", result);
		addEnergyHeatMeter(device, "regeneration", result);
		return result;
	}

	private void addEnergyHeatMeter(ThermalStorage device, String connName, List<Datapoint> result) {
		ThermalConnection thermalConn = device.heatConnections().getSubResource(connName, ThermalConnection.class);
		if(!thermalConn.exists())
			return;
		Datapoint energy = addDatapoint(thermalConn.energySensor().reading(), connName, result);
		Datapoint daily = MeteringEvalUtil.addDailyMeteringEval(energy, null, thermalConn, result, appMan);
		if(daily != null) {
			daily.addToSubRoomLocationAtomic(null, null, connName, false);
			try {
				Datapoint daily2 = result.get(result.size()-2);
				daily2.addToSubRoomLocationAtomic(null, null, connName, false);
				Datapoint monthly2 = result.get(result.size()-1);
				monthly2.addToSubRoomLocationAtomic(null, null, connName, false);
			} catch(IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
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
