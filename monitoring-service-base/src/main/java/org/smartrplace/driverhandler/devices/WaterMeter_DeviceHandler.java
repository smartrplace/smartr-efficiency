package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.FlowSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.smaapis.SmaEnergyBalanceMeasurements;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class WaterMeter_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public WaterMeter_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("VOLUME_0_0", VolumeAccumulatedSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		Datapoint volumeDp = addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		MeteringEvalUtil.addDailyMeteringEval(volumeDp, null, device, result, appMan);
		
		addDatapoint(device.getSubResource("VOLUME_FLOW_0_0", FlowSensor.class).reading(), result);
		addDatapoint(device.getSubResource("VOLTAGE_0_0", ElectricVoltageSensor.class).reading(), result);
		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SensorDevice device = (SensorDevice) appDevice.device();
		
		AlarmingUtiH.setTemplateValues(appDevice, getMainSensorValue(device, appDevice),
				0f, 9999999f, 1, AlarmingUtiH.DEFAULT_NOVALUE_NIGHTLY_MINUTES);
	}

	@Override
	public String getInitVersion() {
		return "B";
	}
	
	@Override
	public String getTableTitle() {
		return "Water Meters";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return WaterMeter_SensorDevicePattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "WTMT";
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, SensorDevice device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {

		DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
		
		vh.stringLabel("InternalName", id, device.getName(), row);
		
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return;
		IntegerResource hotColdType = device.getSubResource("waterMeterType", IntegerResource.class);
		Map<String, String> valuesToSet = new HashMap<>();
		valuesToSet.put("0", "Undefined");
		valuesToSet.put("1", "Cold Water");
		valuesToSet.put("2", "Hot Water");
		vh.dropdown("Type", id, hotColdType, row, valuesToSet);
	}
	
	@Override
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.setColumnTitlesToUse(vh);
		vh.registerHeaderEntry("InternalName");
		vh.registerHeaderEntry("ID");
		vh.registerHeaderEntry(getValueTitle());
		vh.registerHeaderEntry("Tenant");
		vh.registerHeaderEntry("Type");
		vh.registerHeaderEntry("Last Contact");
		vh.registerHeaderEntry("Location");
		vh.registerHeaderEntry("Status");
		vh.registerHeaderEntry("Comment");
		vh.registerHeaderEntry("Plot");
		return true;
	}
	
	@Override
	protected String getValueTitle() {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.getValueTitle();
		return "Counter (m3)";
	}
}
