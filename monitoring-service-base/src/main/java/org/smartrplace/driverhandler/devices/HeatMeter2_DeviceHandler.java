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
import org.ogema.model.metering.HeatMeter;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class HeatMeter2_DeviceHandler extends DeviceHandlerSimple<HeatMeter> {

	public HeatMeter2_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<HeatMeter> getResourceType() {
		return HeatMeter.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(HeatMeter device, InstallAppDevice deviceConfiguration) {
		return device.powerReading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(HeatMeter device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.energyReading(), result);
		addDatapoint(device.connection().inputTemperature().reading(), result);
		addDatapoint(device.connection().outputTemperature().reading(), result);
		addDatapoint(device.connection().flowSensor().reading(), result);
		addDatapoint(device.connection().getSubResource("volumeSensor", VolumeAccumulatedSensor.class).reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Heat Meters BACnet";
	}

	@Override
	protected Class<? extends ResourcePattern<HeatMeter>> getPatternClass() {
		return HeatMeter2_Pattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "HTMB";
	}
}
