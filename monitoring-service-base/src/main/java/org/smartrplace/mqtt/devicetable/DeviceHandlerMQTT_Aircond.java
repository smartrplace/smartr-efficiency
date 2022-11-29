package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.util.DeviceUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.MechanicalFan;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.tablepages.SimpleCheckboxSetpoint;
import org.smartrplace.gui.tablepages.TextFieldSetpoint;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
@SuppressWarnings("serial")
public class DeviceHandlerMQTT_Aircond extends DeviceHandlerSimple<AirConditioner> {
	//private final ApplicationManagerPlus appMan;
	public static final Map<String, String> valuesToSet = new HashMap<>();
	static {
		for(int i=0; i<=3; i++) {
			valuesToSet.put(""+i, DeviceUtil.getAirconAvModeName(i));
		}
	}
	
	public DeviceHandlerMQTT_Aircond(ApplicationManagerPlus appMan) {
		super(appMan, true);
		//this.appMan = appMan;
	}

	@Override
	public Class<AirConditioner> getResourceType() {
		return AirConditioner.class;
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector, DeviceTableConfig config) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final AirConditioner device = (AirConditioner) addNameWidget(object, vh, id, req, row, appMan, config.showOnlyBaseCols());
				Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				if(req != null) {
					TextField setpointSet = new TextFieldSetpoint(mainTable, "setpointSet"+id, alert, 4.5f, 30.5f, req) {
						
						@Override
						protected boolean setValueOnPost(float value, OgemaHttpRequest req) {
							device.temperatureSensor().settings().setpoint().setCelsius(value);
							return true;
						}
						
						@Override
						protected float getValuePreset(OgemaHttpRequest req) {
							return device.temperatureSensor().settings().setpoint().getCelsius();
						}
					};
					row.addCell("Set", setpointSet);
					
					if(!config.showOnlyBaseCols()) {
						SimpleCheckbox onOff = new SimpleCheckboxSetpoint(mainTable, "onOff"+id, alert, req) {
							
							@Override
							protected boolean setValueOnPost(boolean value, OgemaHttpRequest req) {
								device.onOffSwitch().stateControl().setValue(value);
								return true;
							}
							
							@Override
							protected boolean getValuePreset(OgemaHttpRequest req) {
								return device.onOffSwitch().stateFeedback().getValue();
							}
						};
						row.addCell("OnOff", onOff);
					}
					MechanicalFan mfan = device.getSubResource("fan", MechanicalFan.class);
					if(mfan != null && mfan.exists()) {
						TextField fan = new TextFieldSetpoint(mainTable, "fanspeed"+id, alert, 0f, 5f, req) {
							
							@Override
							protected boolean setValueOnPost(float value, OgemaHttpRequest req) {
								mfan.setting().stateControl().setValue(value);
								return true;
							}
							
							@Override
							protected float getValuePreset(OgemaHttpRequest req) {
								return mfan.setting().stateFeedback().getValue();
							}
						};
						row.addCell(WidgetHelper.getValidWidgetId("Fan speed"), fan);
					}
					MultiSwitch opMode = device.getSubResource("operationMode", MultiSwitch.class);
					if(opMode != null && opMode.exists()) {
						TextField opModeF = new TextFieldSetpoint(mainTable, "opMode"+id, alert, 0f, 3f, req) {
							
							@Override
							protected boolean setValueOnPost(float value, OgemaHttpRequest req) {
								opMode.stateControl().setValue(value);
								return true;
							}
							
							@Override
							protected float getValuePreset(OgemaHttpRequest req) {
								return opMode.stateFeedback().getValue();
							}
						};
						row.addCell("OpMode", opModeF);
					}
					
					vh.dropdown("Supported", id, device.operationModesSupported(), row, valuesToSet);
				} else {
					vh.registerHeaderEntry("Set");
					vh.registerHeaderEntry("OnOff");
					vh.registerHeaderEntry("Fan speed");
					vh.registerHeaderEntry("OpMode");
					vh.registerHeaderEntry("Supported");
				}
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				Room deviceRoom = device.location().room();
				Label lastContact = addLastContact(vh, id, req, row, device.temperatureSensor().reading());
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				if(!config.showOnlyBaseCols())
					addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				if(req != null) {
					tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_Aircond.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_Aircond.this.id();
			}			
		};
	}

	@Override
	protected Class<? extends ResourcePattern<AirConditioner>> getPatternClass() {
		return AirconditionerPattern.class;
	}

	public static class SetpointToFeedbackSimSimple implements RoomInsideSimulationBase {
		ResourceValueListener<TemperatureResource> setPointListener = null;
		protected final TemperatureResource setPoint;
		protected final TemperatureResource setPointFeedback;
		ResourceValueListener<TemperatureResource> measurementListener = null;
		long lastUpdate = -1;
		
		@Override
		public void close() {
			if(setPointListener != null)
				setPoint.removeValueListener(setPointListener);
		}

		public SetpointToFeedbackSimSimple(TemperatureResource setPoint, TemperatureResource setPointFeedback,
				final ApplicationManagerPlus appMan, SingleRoomSimulationBase roomSim) {
			this.setPoint = setPoint;
			this.setPointFeedback = setPointFeedback;
			setPointListener = new ResourceValueListener<TemperatureResource>() {
				@Override
				public void resourceChanged(TemperatureResource resource) {
					float value = setPoint.getValue();
					new CountDownDelayedExecutionTimer(appMan.appMan(), 2000l) {
						@Override
						public void delayedExecution() {
							setPointFeedback.setValue(value);
						}
					};
					if(roomSim != null) {
						long now = appMan.getFrameworkTime();
						if(lastUpdate > 0) {
							long stepSize = (now - lastUpdate);
							float joule = (293.15f  - value) * stepSize * 0.1f;
							roomSim.addThermalEnergy(joule);
						}
						lastUpdate = now;
					}
				}
			};
			setPoint.addValueListener(setPointListener, true);
		}
		
	}
	
	public static class TimerSimSimple implements RoomInsideSimulationBase {
		protected final Timer timer;
		
		public TimerSimSimple(Timer timer) {
			this.timer = timer;
		}

		@Override
		public void close() {
			if(timer != null)
				timer.destroy();
		}
	}
	
	@Override
	public List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, AirConditioner resource,
			SingleRoomSimulationBase roomSimulation,
			DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();

		//Return value is currently not used anyways
		if(roomSimulation != null)
			result.add(new SetpointToFeedbackSimSimple(roomSimulation.getTemperature(),
				resource.temperatureSensor().reading(), appMan, roomSimulation));
		result.add(new SetpointToFeedbackSimSimple(resource.temperatureSensor().settings().setpoint(),
				resource.temperatureSensor().deviceFeedback().setpoint(), appMan, null));
		return result;
	}
	
	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(AirConditioner dev, InstallAppDevice deviceConfiguration) {
		//FIXME
		if(Boolean.getBoolean("FIX_AIRCON"))
			dev.activate(true);
		
		//AirConditioner dev = (AirConditioner) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().settings().setpoint()));
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().deviceFeedback().setpoint()));
		result.add(dpService.getDataPointStandard(dev.temperatureSensor().reading()));
		addDatapoint(dev.getSubResource("fan", MechanicalFan.class).setting().stateControl(), result);
		addDatapoint(dev.getSubResource("fan", MechanicalFan.class).setting().stateFeedback(), result);
		addDatapoint(dev.onOffSwitch().stateControl(), result);
		addDatapoint(dev.onOffSwitch().stateFeedback(), result);
		addDatapoint(dev.getSubResource("operationMode", MultiSwitch.class).stateControl(), result);
		addDatapoint(dev.getSubResource("operationMode", MultiSwitch.class).stateFeedback(), result);
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		AirConditioner device = (AirConditioner) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().reading(), 5.0f, 45.0f, 15, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().settings().setpoint(),
				4.5f, 30.5f, 1, -1);
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().deviceFeedback().setpoint(),
				4.5f, 30.5f, 1, 20);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

	@Override
	public SingleValueResource getMainSensorValue(AirConditioner device, InstallAppDevice deviceConfiguration) {
		throw new UnsupportedOperationException("Table generated separately!");
	}

	@Override
	public String getTableTitle() {
		return "Air Conditioners";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}
}
