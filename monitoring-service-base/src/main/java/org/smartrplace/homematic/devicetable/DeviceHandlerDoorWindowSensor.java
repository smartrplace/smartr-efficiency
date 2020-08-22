package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerDoorWindowSensor extends DeviceHandlerBase<DoorWindowSensor> {

	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerDoorWindowSensor(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerDoorWindowSensor";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerDoorWindowSensor.this.getResourceType();
			}

			@Override
			protected String id() {
				return DeviceHandlerDoorWindowSensor.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Window and Door Opening Sensors";
			}

			public DoorWindowSensor addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final DoorWindowSensor device = addNameWidget(object, vh, id, req, row, appMan);
				Label state = vh.booleanLabel("Measured State", id, device.reading(), row, 0);
				addBatteryStatus(vh, id, req, row, device);
				//vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
				Label lastContact = null;
				if(req != null) {
					lastContact = new LastContactLabel(device.reading(), appMan, mainTable, "lastContact"+id, req);
					row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
				} else
					vh.registerHeaderEntry("Last Contact");

				// TODO addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
				Room deviceRoom = device.location().room();
				addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
			 	addSubLocation(object, vh, id, req, row, appMan, deviceRoom);
				Map<String, String> valuesToSet = new HashMap<>();
				valuesToSet.put("0", "unknown");
				valuesToSet.put("1", "Device installed physically");
				valuesToSet.put("10", "Physical installation done including all on-site tests");
				valuesToSet.put("20", "All configuration finished, device is in full operation");
				valuesToSet.put("-10", "Error in physical installation and/or testing (explain in comment)");
				valuesToSet.put("-20", "Error in configuration, device cannot be used/requires action for real usage");
				vh.dropdown("Status", id, object.installationStatus(), row, valuesToSet );
				vh.stringEdit("Comment", id, object.installationComment(), row, alert);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");	
				
				
				if(req != null) {
					state.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				return device;
			}
			public DoorWindowSensor addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof DoorWindowSensor) && (req != null)) return null;
				final DoorWindowSensor device;
				if(req == null)
					device = ResourceHelper.getSampleResource(DoorWindowSensor.class);
				else
					device = (DoorWindowSensor) object.device().getLocationResource();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "WindowSens HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				return device;
			}	

		};
	}

	@Override
	public Class<DoorWindowSensor> getResourceType() {
        return DoorWindowSensor.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		DoorWindowSensor dev = (DoorWindowSensor) installDeviceRes.device();
		if (null == dev) return result;
		result.add(dpService.getDataPointStandard(dev.reading()));
		if(dev.battery().internalVoltage().reading().isActive())
			result.add(dpService.getDataPointStandard(dev.battery().internalVoltage().reading()));
		addtStatusDatapointsHomematic(dev, dpService, result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<DoorWindowSensor>> getPatternClass() {
		return DoorWindowSensorPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		DoorWindowSensor device = (DoorWindowSensor) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.reading(), 0.0f, 1.0f, 1, 30);
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiDevice,
					-30f, -85f, 600, 300);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(device,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			AlarmingUtiH.setTemplateValues(appDevice, rssiPeer,
					-30f, -85f, 600, 300);
	}
}
