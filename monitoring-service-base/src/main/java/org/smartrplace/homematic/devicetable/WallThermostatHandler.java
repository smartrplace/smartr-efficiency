package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.Sensor;
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
	protected SingleValueResource getMainSensorValue(Thermostat device, InstallAppDevice deviceConfiguration) {
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
			row.addCell("Set", setpointSet);
			setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
		} else
			vh.registerHeaderEntry("Set");
		
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
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
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

	private static <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, final List<S> destinations, String altId,
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

}
