package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
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
import org.smartrplace.gui.tablepages.TextFieldSetpoint;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
@SuppressWarnings("serial")
public class DeviceHandlerMQTT_Aircond extends DeviceHandlerSimple<AirConditioner> {
	//private final ApplicationManagerPlus appMan;
	
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
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final AirConditioner device = (AirConditioner) addNameWidget(object, vh, id, req, row, appMan);
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
					
					TextField onOff = new TextFieldSetpoint(mainTable, "onOff"+id, alert, 0f, 1f, req) {
						
						@Override
						protected boolean setValueOnPost(float value, OgemaHttpRequest req) {
							device.onOffSwitch().stateControl().setValue(value>0.5f);
							return true;
						}
						
						@Override
						protected float getValuePreset(OgemaHttpRequest req) {
							return device.onOffSwitch().stateFeedback().getValue()?1:0;
						}
					};
					row.addCell("OnOff", onOff);

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
					/*TextField setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
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
					};*/
				} else {
					vh.registerHeaderEntry("Set");
					vh.registerHeaderEntry("Fan speed");
					vh.registerHeaderEntry("OpMode");
				}
				
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				Room deviceRoom = device.location().room();
				Label lastContact = addLastContact(vh, id, req, row, device.temperatureSensor().reading());
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
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

			@Override
			public String getTableTitle() {
				return "Air Conditioners";
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
				4.5f, 30.5f, 1, 1500);
		AlarmingUtiH.setTemplateValues(appDevice, device.temperatureSensor().deviceFeedback().setpoint(),
				4.5f, 30.5f, 1, 20);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

	@Override
	protected SingleValueResource getMainSensorValue(AirConditioner device, InstallAppDevice deviceConfiguration) {
		throw new UnsupportedOperationException("Table generated separately!");
	}

	@Override
	public String getTableTitle() {
		return "Air Conditioners";
	}
}
