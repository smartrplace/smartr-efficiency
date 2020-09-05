package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
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
public class TemperatureOrHumiditySensorDeviceHandler extends DeviceHandlerBase<SensorDevice> {

	private final ApplicationManagerPlus appMan;
	
	public TemperatureOrHumiditySensorDeviceHandler(ApplicationManagerPlus appMan) {
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
				return TemperatureOrHumiditySensorDeviceHandler.this.getResourceType();
			}

			@Override
			protected String id() {
				return TemperatureOrHumiditySensorDeviceHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Temperature and Humidity Sensors";
			}

			public SensorDevice addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final SensorDevice device2 = addNameWidget(object, vh, id, req, row, appMan).getLocationResource();
				final SensorElements device = getElements(device2);
				Label stateTemp = null;
				Label stateHum = null;
				Label lastContact = null;
				if(device.tempSens != null) {
					stateTemp = vh.floatLabel("Temperature", id, device.tempSens.reading(), row, "%.1f");
					lastContact = new LastContactLabel(device.tempSens.reading(), appMan, mainTable, "lastContact"+id, req);
					row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
				} else if(req == null) {
					vh.registerHeaderEntry("Temperature");
				}
				if(device.humSens != null) {
					stateHum = vh.floatLabel("Humidity", id, device.humSens.reading(), row, "%.1f");
					if(lastContact == null) {
						lastContact = new LastContactLabel(device.humSens.reading(), appMan, mainTable, "lastContact"+id, req);
						row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);						
					}
				} else if(req == null) {
					vh.registerHeaderEntry("Humidity");
					vh.registerHeaderEntry("Last Contact");
				}
				addBatteryVoltage(vh, id, req, row, device2);
				/*VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(device2,
						"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
				if(batteryVoltage != null)
					vh.floatLabel("Battery", id, batteryVoltage, row, "%.1f#min:0.1");
				else if(req == null)
					vh.registerHeaderEntry("Battery");*/
				addBatteryStatus(vh, id, req, row, device2);
				/*BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(device2,
						"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
				if(batteryStatus != null)
					vh.booleanLabel("Bat.Low", id, batteryStatus, row, 0);
				else if(req == null)
					vh.registerHeaderEntry("Bat.Low");*/

				// TODO addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
				Room deviceRoom = device2.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");	
				
				
				if(stateTemp != null) {
					stateTemp.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				if(stateHum != null) {
					stateHum.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				if(lastContact != null) {
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				return device2;
			}
			
			class SensorElements {
				public TemperatureSensor tempSens;
				public HumiditySensor humSens;
			}
			SensorElements getElements(SensorDevice dev) {
				SensorElements result = new SensorElements();
				//List<Resource> allSubs = dev.sensors().getSubResources(false);
				List<TemperatureSensor> tss = dev.sensors().getSubResources(TemperatureSensor.class, false);
				if(tss.size() == 1)
					result.tempSens = tss.get(0);
				List<HumiditySensor> hss = dev.sensors().getSubResources(HumiditySensor.class, false);
				if(hss.size() == 1)
					result.humSens = hss.get(0);
				return result ;
			}
			
			@Override
			public SensorDevice addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof DoorWindowSensor) && (req != null)) return null;
				final SensorDevice device;
				if(req == null) {
					device = ResourceHelper.getSampleResource(SensorDevice.class);
					//device.sensors().addDecorator("sampleTemp", TemperatureSensor.class).activate(false);
					//device.sensors().addDecorator("sampleHum", HumiditySensor.class).activate(false);
				} else
					device = ((SensorDevice) object.device()).getLocationResource();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "TH HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = getName(object, appManPlus); //ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				return device;
			}	

		};
	}

	@Override
	public Class<SensorDevice> getResourceType() {
        return SensorDevice.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		SensorDevice dev = (SensorDevice) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		for(Sensor sens: dev.sensors().getAllElements()) {
			if(sens.reading() instanceof SingleValueResource)
				addDatapoint((SingleValueResource) sens.reading(), result, dpService);			
		}
		addtStatusDatapointsHomematic(dev, dpService, result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return TemperatureOrHumiditySensorPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
	
	@Override
	public String getDeviceTypeShortId(InstallAppDevice device, DatapointService dpService) {
		return "TH";
	}

}
