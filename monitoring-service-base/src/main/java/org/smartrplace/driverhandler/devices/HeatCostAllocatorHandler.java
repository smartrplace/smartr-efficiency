package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.buildingtechnology.ElectricLight;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class HeatCostAllocatorHandler extends DeviceHandlerSimple<SensorDevice> {

	public HeatCostAllocatorHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("hcaEnergy", EnergyAccumulatedSensor.class).reading();
	}

	@Override
	public String getTableTitle() {
		return "Heat Cost Allocators";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		//addDatapoint(device.getSubResource("hcaEnergy", EnergyAccumulatedSensor.class).reading(), result, dpService);
		FloatResource factorRes = device.getSubResource("factor", FloatResource.class);
		if(ValueResourceHelper.setIfNew(factorRes, 1.0f))
			factorRes.activate(false);
		addDatapoint(factorRes, "Factor", result);
		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SensorDevice device = (SensorDevice) appDevice.device();
		
		AlarmingUtiH.setTemplateValues(appDevice, device.getSubResource("hcaEnergy", EnergyAccumulatedSensor.class).reading(),
				0f, 9999999f, 1, 2880);
	}

	@Override
	public String getInitVersion() {
		return "A";
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, SensorDevice device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		FloatResource factorRes = device.getSubResource("factor", FloatResource.class);
		vh.floatEdit("Factor", id, factorRes, row, alert, 0.0f, 999f, "Factor values from 0.0 to 999.0 allowed!", 0);
	
		vh.stringLabel("InternalName", id, device.getName(), row);
		//DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
	}
	
	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return HeatCostAllocatorPattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "HCA";
	}
	
	@Override
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.setColumnTitlesToUse(vh);
		vh.registerHeaderEntry("InternalName");
		vh.registerHeaderEntry("ID");
		vh.registerHeaderEntry(getValueTitle());
		vh.registerHeaderEntry("Factor");
		vh.registerHeaderEntry("Room");
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
		return "Counter";
	}

}
