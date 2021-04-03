package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.alarming.extension.model.BatteryAlarmExtensionData;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField.Interval;

public class BatteryPage extends MainPage {

	DeviceTableBase devTable;
	
	@Override
	protected String getHeader() {return "Smartrplace Hardware InstallationApp Expert";}

	public BatteryPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, false);
		finishConstructor();
		
		StaticTable configTable = new StaticTable(3, 2);
		TimeResourceTextField retardEdit = new TimeResourceTextField(page, "retardEdit", Interval.hours);
		BatteryAlarmExtensionData config = ResourceListHelper.getOrCreateNamedElementFlex(controller.appConfigData.alarmingConfig(),
				BatteryAlarmExtensionData.class);
		retardEdit.selectDefaultItem(config.alarmRetard());
		configTable.setContent(0, 0, "Low time before sending alarm (hours)").setContent(0, 1, retardEdit);
		page.append(configTable);
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			@Override
			public String getLineId(InstallAppDevice object) {
				return object.getName()+"_BAT";
			}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final PhysicalElement device2 = addNameWidget(object, vh, id, req, row, appMan).getLocationResource();
				AddBatteryVoltageResult voltageLab = addBatteryVoltage(vh, id, req, row, device2);
				Label lastContactVoltage = null;
				Label lastContactStatus = null;
				if(req == null)
					vh.registerHeaderEntry("Last Voltage");
				else if(voltageLab != null)
					lastContactVoltage = addLastContact("Last Voltage", vh, "LV"+id, req, row, voltageLab.reading);
				AddBatteryVoltageResult statusLab = addBatteryStatus(vh, id, req, row, device2);
				if(req == null)
					vh.registerHeaderEntry("Last Status");
				else if(statusLab != null)
					lastContactStatus = addLastContact("Last Status", vh, "LStat"+id, req, row, statusLab.reading);
				Room deviceRoom = device2.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
			 	addSubLocation(object, vh, id, req, row);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");	
				
				if(voltageLab != null)
					voltageLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(statusLab != null)
					statusLab.label.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(lastContactVoltage != null)
					lastContactVoltage.setPollingInterval(DEFAULT_POLL_RATE, req);
				if(lastContactStatus != null)
					lastContactStatus.setPollingInterval(DEFAULT_POLL_RATE, req);
			}
			
			@Override
			public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				return addNameWidgetStatic(object, vh, id, req, row, appManPlus);
			}	

			@Override
			protected String id() {
				return "BatteryDetails";
			}
			
			@Override
			public String getTableTitle() {
				return "Battery State Overview";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return PhysicalElement.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, installFilterDrop, req);
				//List<InstallAppDevice> all = appSelector.getDevicesSelected();
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					PhysicalElement device2 = dev.device().getLocationResource();
					VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(device2,
							"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
					if(batteryVoltage != null)
						result.add(dev);
				}
				return result;
			}
		};
		devTable.triggerPageBuild();
		installFilterDrop.registerDependentWidget(devTable.getMainTable());
		
	}
	
	public static PhysicalElement addNameWidgetStatic(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManagerPlus appManPlus) {
		final PhysicalElement device;
		if(req == null) {
			device = ResourceHelper.getSampleResource(SensorDevice.class);
		} else
			device = object.device().getLocationResource();
		final String name;
		name = DeviceTableRaw.getName(object, appManPlus); //ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		return device;
	}
}
