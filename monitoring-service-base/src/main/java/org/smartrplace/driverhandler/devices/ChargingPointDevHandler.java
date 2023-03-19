package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.storage.ChargingPoint;
import org.ogema.model.devices.storage.ElectricityChargingStation;
import org.ogema.timeseries.eval.simple.mon3.MeteringEvalUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class ChargingPointDevHandler extends DeviceHandlerSimple<ChargingPoint> {
	
	public ChargingPointDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}
	
	@Override
	public Class<ChargingPoint> getResourceType() {
		return ChargingPoint.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		if (device.electricityConnection().powerSensor().reading().isActive()) {
			return device.electricityConnection().powerSensor().reading();
		}
		return device.setting().stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ChargingPoint device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.setting().stateControl(), result);
		addDatapoint(device.setting().stateFeedback(), result);
		addDatapoint(device.battery().chargeSensor().reading(), result);
		
		addDatapoint(device.electricityConnection().powerSensor().reading(), "Actual Power", result);
		Datapoint energyDp = addDatapoint(device.electricityConnection().energySensor().reading(), "Total Energy", result);
		Resource isPlugged = device.getSubResource("isPlugged");
		if (isPlugged != null && isPlugged instanceof IntegerResource) {
			addDatapoint((IntegerResource) isPlugged, "Plugged", result);
		}
		Resource isCharging = device.getSubResource("isCharging");
		if (isCharging != null && isCharging instanceof IntegerResource) {
			addDatapoint((IntegerResource) isCharging, "Charging", result);
		}
		
		if(energyDp != null) {
			MeteringEvalUtil.addDailyMeteringEval(energyDp, null, device.electricityConnection(), result, appMan);			
		}

		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ChargingPoint device = (ChargingPoint) appDevice.device();
		
		AlarmingUtiH.setTemplateValues(appDevice, getMainSensorValue(device, appDevice),
				0f, 70000f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
	}

	@Override
	public String getInitVersion() {
		return "A";
	}
	
	
	@Override
	public String getTableTitle() {
		return "Charging Stations";
	}

	@Override
	protected Class<? extends ResourcePattern<ChargingPoint>> getPatternClass() {
		return ChargingPointPattern.class;
	}

	@Override
	public ComType getComType() {
		return ComType.IP;
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, ChargingPoint device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {

		DeviceTableRaw.addTenantWidgetStatic(vh, id, req, row, appMan, device);
		
		ElectricityChargingStation parent = ResourceHelper.getFirstParentOfType(device, ElectricityChargingStation.class);
		if(parent != null)
			vh.stringLabel("InternalName", id, parent.getName(), row);
		else
			vh.stringLabel("InternalName", id, device.getName(), row);
	}

	@Override
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return super.setColumnTitlesToUse(vh);
		vh.registerHeaderEntry("InternalName");
		vh.registerHeaderEntry("ID");
		vh.registerHeaderEntry(getValueTitle());
		vh.registerHeaderEntry("Tenant");
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
		return "Power (W)";
	}
}
