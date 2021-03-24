package org.smartrplace.driverhandler.more;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.mqtt.devicetable.DeviceHandlerMQTT_Aircond;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

public class VirtualThermostatDeviceHandler extends DeviceHandlerSimple<Thermostat> {

	protected static final long MIN_HYSTERSIS_TIME = 60000;
	protected static final float MIN_HYSTERSIS_DIFF = 1.5f;

	public VirtualThermostatDeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
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
	}
	
	@Override
	protected Collection<Datapoint> getDatapoints(Thermostat device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.temperatureSensor().deviceFeedback().setpoint(), result);
		addDatapoint(device.temperatureSensor().settings().setpoint(), result);
		Room room = device.location().room();
		List<OnOffSwitch> onOffs = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), OnOffSwitch.class, room);
		for(OnOffSwitch onOff: onOffs) {
			addDatapoint(onOff.stateControl(), result);
			addDatapoint(onOff.stateFeedback(), result);
		}
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "Virtual thermostats for electricity controlled heating";
	}

	@Override
	protected Class<? extends ResourcePattern<Thermostat>> getPatternClass() {
		return VirtualThermostatPattern.class;
	}

	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			Thermostat deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();
		
		Timer timer = appMan.appMan().createTimer(10000, new TimerListener() {
			Boolean prevState = null;
			long lastSwitch = -1;
			float lastSetp = -1;
			
			@Override
			public void timerElapsed(Timer arg0) {
				long now = arg0.getExecutionTime();
				if((now - lastSwitch) < MIN_HYSTERSIS_TIME)
					return;
				TemperatureResource tempSens = deviceResource.temperatureSensor().reading(); //getSubResource("config", VirtualThermostatConfig.class).roomSensor();
				Room room = deviceResource.location().room();
				//if we would allow measurements of thermostats we would also need an offset
				if(!tempSens.exists() || (!room.exists()) ||
						(!room.equalsLocation(ResourceUtils.getDeviceRoom(tempSens)))) {
					List<TemperatureSensor> all = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), TemperatureSensor.class, room);
					for(TemperatureSensor ts: all) {
						if(ResourceHelper.hasParentAboveType(ts, Thermostat.class) < 0) {
							tempSens.setAsReference(ts.reading());
							break;
						}
					}
				}
				float mes = tempSens.getValue();
				float setp = deviceResource.temperatureSensor().settings().setpoint().getValue();
				boolean newStateRaw;
				if(prevState == null)
					newStateRaw= (setp > mes);
				else
					newStateRaw= prevState?(setp >= (mes-MIN_HYSTERSIS_DIFF)):(setp > (mes+MIN_HYSTERSIS_DIFF));						

				// WE COULD ALSO HAVE SIMPLER FEEDBACK: For now we always return feedback as long as the timer is running and we are not in
				//hysteresis time. In the future we want to have the switch feedback, though
				List<OnOffSwitch> onOffs = null;
				if(setp != lastSetp) {
					boolean allOnOffCorrect = true;
					onOffs = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), OnOffSwitch.class, room);
					for(OnOffSwitch onOff: onOffs) {
						if(onOff.stateFeedback().getValue() != newStateRaw) {
							allOnOffCorrect = false;
							break;
						}
					}
					if(allOnOffCorrect) {
						deviceResource.temperatureSensor().deviceFeedback().setpoint().setValue(setp);
						lastSetp = setp;
					}
				}					
					
				if(prevState != null && (newStateRaw == prevState)) {
					return;
				}
				if(!room.exists())
					return;
				if(onOffs == null)
					onOffs = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), OnOffSwitch.class, room);
				for(OnOffSwitch onOff: onOffs) {
					onOff.stateControl().setValue(newStateRaw);
				}
				prevState = newStateRaw;
				lastSwitch = now;
			}
		});
		result.add(new DeviceHandlerMQTT_Aircond.TimerSimSimple(timer));
		return result;
	}
	
	@Override
	public List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, Thermostat deviceResource,
			SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		
		//TODO: Use same code with standard DeviceHandlerThermostat
		if(Boolean.getBoolean("org.smartrplace.homematic.devicetable.sim.extended.thermostat")) {
			if(ValueResourceHelper.setIfNew(deviceResource.temperatureSensor().reading(), 20+273.15f))
				deviceResource.temperatureSensor().activate(true);
			List<RoomInsideSimulationBase> result = new ArrayList<>();

			TemperatureResource mesRes = deviceResource.temperatureSensor().reading();
			TemperatureResource setpRes = deviceResource.temperatureSensor().deviceFeedback().setpoint();
			final double factor = 2.0/TimeProcUtil.HOUR_MILLIS;
			Timer timer = appMan.appMan().createTimer(TimeProcUtil.MINUTE_MILLIS, new TimerListener() {
				boolean jitter = false;
				long lastTime = -1;
				
				@Override
				public void timerElapsed(Timer arg0) {
					long now = arg0.getExecutionTime();
					if(lastTime <= 0) {
						lastTime = now - 60000;
					}
					float mes = mesRes.getValue();
					float setp = setpRes.getValue();
					float newMes;
					long step = now - lastTime;
					float diff = setp - mes;
					newMes = (float) (mes + diff*step*factor);
					/*if(setp > mes) {
						newMes = (float) (mes + step*factor);
					} else
						newMes =  (float) (mes - step*factor);*/
					if(jitter)
						newMes += 0.1;
					else
						newMes -= 0.1;
					mesRes.setValue(newMes);
					setpRes.setValue(setp);
					jitter = !jitter;
					lastTime = now;
				}
			});
			result.add(new DeviceHandlerMQTT_Aircond.TimerSimSimple(timer));
			return result;
		}
		
		return super.startSimulationForDevice(device, deviceResource, roomSimulation, dpService);
	}
}
