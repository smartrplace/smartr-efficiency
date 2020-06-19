package org.smartrplace.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerThermostat extends DeviceHandlerBase<Thermostat> {

	private final ApplicationManager appMan;

	public DeviceHandlerThermostat(ApplicationManager appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerThermostat";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerThermostat.this.getResourceType();
			}

			@Override
			protected String id() {
				return DeviceHandlerThermostat.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Thermostats*";
			}
			
			public Thermostat addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final Thermostat device;
				if(req == null)
					device = ResourceHelper.getSampleResource(Thermostat.class);
				else
					device = (Thermostat) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "Thermostat HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				if(req != null) {
					TextField setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							setValue(String.format("%.1f", device.temperatureSensor().deviceFeedback().setpoint().getCelsius()), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							String val = getValue(req);
							val = val.replaceAll("[^\\d.]", "");
							try {
								float value  = Float.parseFloat(val);
								if(value < 4.5f || value> 30.5f) {
									alert.showAlert("Allowed range: 4.5 to 30°C", false, req);
								} else
									device.temperatureSensor().settings().setpoint().setCelsius(value);
							} catch (NumberFormatException | NullPointerException e) {
								if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
								return;
							}
						}
					};
					row.addCell("Set", setpointSet);
				} else
					vh.registerHeaderEntry("Set");
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
				Label lastContact = null;
				if(req != null) {
					lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
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
					tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				return device;
			}
			
		};
	}

	@Override
	public Class<Thermostat> getResourceType() {
		return Thermostat.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		Thermostat dev = (Thermostat) installDeviceRes.device();
		if (null == dev) return result;
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().reading()));
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().settings().setpoint()));
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<Thermostat>> getPatternClass() {
		return ThermostatPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

}
