package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.BatteryEvalBase;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.resource.DefaultResourceTemplate;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;

public class WallThermostatHandler extends DeviceHandlerSimple<Thermostat> {

	public WallThermostatHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<Thermostat> getResourceType() {
		return Thermostat.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(Thermostat device, InstallAppDevice deviceConfiguration) {
		return device.temperatureSensor().reading();
	}

	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, Thermostat device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
		if(req != null) {
			TextField setpointSet = new TextField(vh.getParent(), "setpointSet"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(String.format("%.1f", device.temperatureSensor().settings().setpoint().getCelsius()), req);
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
			row.addCell("SetpSet", setpointSet);
			setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
		} else
			vh.registerHeaderEntry("SetpSet");
		Label batLab = vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
		if(req != null)
			BatteryEvalBase.addBatteryStyle(batLab, device.battery().internalVoltage().reading().getValue(), false, req);
		
		Room deviceRoom = device.location().room();
		
		if(req == null) {
			vh.registerHeaderEntry("Room");
			return;
		}
		List<Room> rooms = new ArrayList<>();
		rooms.add(deviceRoom);
		Resource parent = device.getLocationResource().getParent();
		if(parent != null) {
			List<SensorDevice> allSensDev = parent.getSubResources(SensorDevice.class, false);
			for(SensorDevice sensDev: allSensDev) {
				if(!sensDev.getName().startsWith("WEATHER"))
					continue;
				rooms.add(sensDev.location().room());
			}
		}

		ResourceDropdown<Room> drop = referenceDropdownFixedChoice(WidgetHelper.getValidWidgetId("Room"+id),
				rooms, "",
				DeviceTableRaw.roomsToSet, Room.class, vh.getParent(), req);
		row.addCell("Room", drop);
	}
	
	@Override
	protected Collection<Datapoint> getDatapoints(Thermostat device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		Datapoint dpRef = addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.temperatureSensor().deviceFeedback().setpoint(), result);
		addDatapoint(device.temperatureSensor().settings().setpoint(), result);
		addDatapoint(device.getSubResource("humiditySensor", HumiditySensor.class).reading(), result, dpService);
		addtStatusDatapointsHomematic(device, dpService, result);
		Resource parent = device.getLocationResource().getParent();
		if(parent != null) {
			List<SensorDevice> allSensDev = parent.getSubResources(SensorDevice.class, false);
			for(SensorDevice sensDev: allSensDev) {
				if(!sensDev.getName().startsWith("WEATHER"))
					continue;
				for(Sensor sens: sensDev.sensors().getAllElements()) {
					if(sens.reading() instanceof SingleValueResource)
						addDatapoint((SingleValueResource) sens.reading(), result, dpService);			
				}
			}
		}
		
		DeviceHandlerThermostat.addMemoryDps(dpRef, deviceConfiguration, result, dpService, appMan.getResourceAccess(), false, this);
		DeviceHandlerThermostat.addSetpReactDp(dpRef, deviceConfiguration, device, result, dpService, appMan.getResourceAccess(), this);
		
		return result;
	}

	@Override
	public List<Resource> devicesControlled(InstallAppDevice iad) {
		List<Resource> result = new ArrayList<>();
		PhysicalElement device = iad.device();
		result.add(device);
		
		Resource parent = device.getLocationResource().getParent();
		if(parent != null) {
			List<SensorDevice> allSensDev = parent.getSubResources(SensorDevice.class, false);
			for(SensorDevice sensDev: allSensDev) {
				if(!sensDev.getName().startsWith("WEATHER"))
					continue;
				result.add(sensDev);
			}
		}
		return result;
	}
	
	@Override
	public String getTableTitle() {
		return "Wall Thermostats";
	}

	@Override
	protected Class<? extends ResourcePattern<Thermostat>> getPatternClass() {
		return WallThermostatPattern.class;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		Thermostat dev = DeviceHandlerThermostat.initAlarmingForDeviceThermostatCommon(appDevice, appConfigData);
		AlarmConfiguration ac = AlarmingUtiH.getAlarmingConfiguration(appDevice, dev.temperatureSensor().reading());
		if(ac != null)
			ac.maxIntervalBetweenNewValues().setValue(AlarmingUtiH.DEFAULT_NOVALUE_FORHOURLY_MINUTES);
		
		ac = AlarmingUtiH.getAlarmingConfiguration(appDevice, dev.temperatureSensor().deviceFeedback().setpoint());
		if(ac != null)
			ac.maxIntervalBetweenNewValues().setValue(AlarmingUtiH.DEFAULT_NOVALUE_FORHOURLY_MINUTES);
		
		ac = AlarmingUtiH.getAlarmingConfiguration(appDevice, dev.valve().setting().stateFeedback());
		if(ac != null)
			ac.maxIntervalBetweenNewValues().setValue(AlarmingUtiH.DEFAULT_NOVALUE_FORHOURLY_MINUTES);
	}
	
	@Override
	public String getInitVersion() {
		return "K";
	}
	
	public static <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, final List<S> destinations, String altId,
			final Map<S, String> valuesToSet, final Class<S> resourceType, OgemaWidget mainTable, OgemaHttpRequest req) {
		@SuppressWarnings("serial")
		ResourceDropdown<S> myDrop = new ResourceDropdown<S>(mainTable, widgetId, req) {
			@SuppressWarnings("unchecked")
			public void onGET(OgemaHttpRequest req) {
				//S source = getResource(sva, req, resourceType);
				S destination = destinations.get(0);
				if(destination.exists())
					selectItem((S) destination.getLocationResource(), req);
				else
					selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				for(S destination: destinations) {
					if(!destination.exists()) {
						destination.create();
						destination.activate(true);
					}
					S selection = getSelectedItem(req);
					if(selection == null) destination.delete();
					else destination.setAsReference(selection);
				}
			}
		};
		DefaultResourceTemplate<S> displayTemplate = new DefaultResourceTemplate<S>() {
			@Override
			public String getLabel(S object, OgemaLocale locale) {
				String result = valuesToSet.get(object);
				if(result != null) return result;
				return super.getLabel(object, locale);
			}
		};
		myDrop.setTemplate(displayTemplate);
		myDrop.setDefaultItems(valuesToSet.keySet());
		myDrop.setDefaultAddEmptyOption(true, "(not set)");
		return myDrop;
	}

	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			Thermostat deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		return DeviceHandlerThermostat.startSupportingLogicForDevice(device, deviceResource, roomSimulation, dpService, appMan);
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "WT";
	}
	
	@Override
	public List<SetpointData> getSetpointData(Thermostat device,
			InstallAppDevice deviceConfiguration) {
		if(deviceConfiguration == null)
			return Collections.emptyList();
		List<SetpointData> result = new ArrayList<>();
		result.add(new SetpointData(device.temperatureSensor().settings().setpoint(),
				device.temperatureSensor().deviceFeedback().setpoint()));
		return result;
	}
}
