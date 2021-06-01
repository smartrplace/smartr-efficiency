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
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.homematic.devicetable.TemperatureOrHumiditySensorDeviceHandler.SensorElements;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.HmCentralManager;

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

public class CO2SensorHmHandler extends DeviceHandlerSimple<CO2Sensor> {

	public CO2SensorHmHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<CO2Sensor> getResourceType() {
		return CO2Sensor.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(CO2Sensor device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, CO2Sensor device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		if(req == null) {
			vh.registerHeaderEntry("StateFB");
			vh.registerHeaderEntry("Control");
			vh.registerHeaderEntry("Room");
			return;
		}
		final SensorElements sens = getElements(device);
		if(sens.onOffSwitch != null) {
			final OnOffSwitch box = sens.onOffSwitch;
			Label stateFB = vh.booleanLabel("StateFB", id, box.stateFeedback(), row, 0);
			vh.booleanEdit("Control", id, box.stateControl(), row);
			
			if(stateFB != null) {
				stateFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
			}
		}
		
		Room deviceRoom = device.location().room();
		
		List<Room> rooms = new ArrayList<>();
		rooms.add(deviceRoom);
		Resource parent = device.getLocationResource().getParent();
		if(parent != null) {
			List<SensorDevice> allSensDev = parent.getSubResources(SensorDevice.class, false);
			for(SensorDevice sensDev: allSensDev) {
				rooms.add(sensDev.location().room());
			}
			if(sens.onOffSwitch != null) {
				rooms.add(sens.onOffSwitch.location().room());
			}
		}

		ResourceDropdown<Room> drop = WallThermostatHandler.referenceDropdownFixedChoice(WidgetHelper.getValidWidgetId("Room"+id),
				rooms, "",
				DeviceTableRaw.roomsToSet, Room.class, vh.getParent(), req);
		row.addCell("Room", drop);

	}
	
	@Override
	protected Collection<Datapoint> getDatapoints(CO2Sensor device, InstallAppDevice deviceConfiguration) {
		final SensorElements sens = getElements(device);

		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		if(sens.tempSens != null)
			addDatapoint(sens.tempSens.reading(), result);
		if(sens.humSens != null)
			addDatapoint(sens.humSens.reading(), result);
		if(sens.onOffSwitch != null) {
			addDatapoint(sens.onOffSwitch.stateControl(), result);
			addDatapoint(sens.onOffSwitch.stateFeedback(), result);
		}
		addtStatusDatapointsHomematic(device, dpService, result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "CO2Sensors Homematic IP";
	}

	@Override
	protected Class<? extends ResourcePattern<CO2Sensor>> getPatternClass() {
		return CO2SensorHmPattern.class;
	}

	class SensorElements {
		public TemperatureSensor tempSens;
		public HumiditySensor humSens;
		public OnOffSwitch onOffSwitch;
	}
	SensorElements getElements(CO2Sensor co2) {
		Resource dev = co2.getParent();
		SensorElements result = new SensorElements();
		if(dev == null)
			return result;
		//List<Resource> allSubs = dev.sensors().getSubResources(false);
		List<TemperatureSensor> tss = dev.getSubResources(TemperatureSensor.class, true);
		if(tss.size() == 1)
			result.tempSens = tss.get(0);
		List<HumiditySensor> hss = dev.getSubResources(HumiditySensor.class, true);
		if(hss.size() == 1)
			result.humSens = hss.get(0);
		List<OnOffSwitch> oos = dev.getSubResources(OnOffSwitch.class, false);
		if(hss.size() == 1)
			result.onOffSwitch = oos.get(0);
		return result ;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		CO2Sensor co2 = (CO2Sensor) appDevice.device().getLocationResource();
		AlarmingUtiH.setTemplateValues(appDevice, co2.reading(), 300f, 4000f, 15, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		
		final SensorElements device = getElements(co2);
		if(device.tempSens != null) {
			AlarmingUtiH.setTemplateValues(appDevice, device.tempSens.reading(), 5.0f, 35.0f, 15, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		}
		if(device.humSens != null)
			AlarmingUtiH.setTemplateValues(appDevice, device.humSens.reading(),
					0.0f, 1.0f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.addAlarmingHomematic(co2, appDevice);
		appDevice.alarms().activate(true);
	}
	
	@Override
	public String getInitVersion() {
		return "A";
	}
}
