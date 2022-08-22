package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.generators.HeatPump;
import org.ogema.model.sensors.GenericBinarySensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


public class HeatPumpDevHandler extends DeviceHandlerSimple<HeatPump> {
	
	public static class HeatPumpPattern extends ResourcePattern<HeatPump> {
		
		public HeatPumpPattern(Resource model) {
			super(model);
		}
	}
	
	public HeatPumpDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}
	
	@Override
	public Class<HeatPump> getResourceType() {
		return HeatPump.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(HeatPump device, InstallAppDevice deviceConfiguration) {
		return device.thermalConnection().inputTemperature().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(HeatPump device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.thermalConnection().inputTemperature().reading(), result);
		addDatapoint(device.thermalConnection().outputTemperature().reading(), result);
		addDatapoint(device.thermalConnection().energySensor().reading(), "Thermal Energy", result);
		addDatapoint(device.electricityConnection().energySensor().reading(), "Electric Energy", result);
		
		addDatapoint(device.electricityConnection().powerSensor().reading(), "Electric Power Consumption", result);
		addDatapoint(device.thermalConnection().powerSensor().reading(), "Thermal Power Output", result);
		Resource cop = device.getSubResource("cop");
		if (cop != null && cop instanceof GenericFloatSensor) {
			addDatapoint(((GenericFloatSensor) cop).reading(), "CoP", result);
		}
		
		//Refridgerator and Recooler datapoints
		addDatapoint(device.onOffSwitch().stateControl(), result);
		addDatapoint(device.onOffSwitch().stateFeedback(), result);
		addDatapoint(device.setting().stateControl(), result);
		addDatapoint(device.setting().stateFeedback(), result);
		addDatapoint(device.getSubResource("malfunction", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.getSubResource("malfunctionGas", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.getSubResource("operating", GenericBinarySensor.class).reading(), result);
		
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Heat Pumps";
	}

	@Override
	protected Class<? extends ResourcePattern<HeatPump>> getPatternClass() {
		return HeatPumpPattern.class;
	}

	@Override
	public ComType getComType() {
		return ComType.IP;
	}
}
