package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.FlowSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class HeatMeter_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public HeatMeter_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("POWER_0_0", PowerSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		Datapoint energyDp = addDatapoint(device.getSubResource("ENERGY_0_0", EnergyAccumulatedSensor.class).reading(), result);
		addDatapoint(device.getSubResource("FLOW_TEMPERATURE_0_0",TemperatureSensor.class).reading(), result);
		addDatapoint(device.getSubResource("RETURN_TEMPERATURE_0_0", TemperatureSensor.class).reading(), result);
		addDatapoint(device.getSubResource("VOLUME_0_0", VolumeAccumulatedSensor.class).reading(), result);
		addDatapoint(device.getSubResource("VOLUME_FLOW_0_0", FlowSensor.class).reading(), result);
		addDatapoint(device.getSubResource("VOLTAGE_0_0", ElectricVoltageSensor.class).reading(), result);
		
		MeteringEvalUtil.addDailyMeteringEval(energyDp, null, device, result, appMan);
		
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Heat Meters";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return HeatMeter_SensorDevicePattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "HTMR";
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, SensorDevice device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {

		DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
		
		vh.stringLabel("InternalName", id, device.getName(), row);

		if(req == null) {
			vh.registerHeaderEntry("Counter");
			return;
		}
		EnergyResource counterRes = device.getSubResource("ENERGY_0_0", EnergyAccumulatedSensor.class).reading();
		if(counterRes.exists())
			vh.floatLabel("Counter", id, counterRes, row, "%.1f");
	}
}
