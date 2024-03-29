package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.communication.CommunicationStatus;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_SingleSwBox extends DeviceHandlerSimple<SingleSwitchBox> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerMQTT_SingleSwBox(ApplicationManagerPlus appMan) {
		super(appMan, true);
		this.appMan = appMan;
	}
	
	@Override
	public Class<SingleSwitchBox> getResourceType() {
		return SingleSwitchBox.class;
	}
	
	@Override
	protected void addMoreValueWidgets(InstallAppDevice object, SingleSwitchBox device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan, Alert alert) {
		PowerResource power = device.electricityConnection().powerSensor().reading();
		TemperatureResource temperature = device.getSubResource("temperatureSensor", TemperatureSensor.class).reading();
		if((req != null) && (!power.isActive()) && (!temperature.isActive()))
			return;
		Label stateFB = vh.booleanLabel("StateFB", id, device.onOffSwitch().stateFeedback(), row, 0);
		Label lastFB = addLastContact("Last FB", vh, "LFB"+id, req, row, device.onOffSwitch().stateFeedback());
		if(stateFB != null)
			stateFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
		if(lastFB != null)
			lastFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);

		vh.booleanEdit("Control", id, device.onOffSwitch().stateControl(), row);
		
		if(req == null) {
			vh.registerHeaderEntry("clientUrl");
			return;
		}

		StringResource clientUrlEth = device.getSubResource("ipAddressEth", StringResource.class);
		StringResource clientUrlWifi = device.getSubResource("ipAddressWifi", StringResource.class);
		StringResource clientUrlRes;
		boolean alsoWifi = false;
		if(clientUrlEth.exists()) {
			clientUrlRes = clientUrlEth;
			if(clientUrlWifi.exists())
				alsoWifi = true;
		} else
			clientUrlRes = clientUrlWifi;
		if(clientUrlRes.exists()) {
			String showVal = clientUrlRes.getValue();
			if(alsoWifi)
				showVal += "+Wifi";
			vh.stringLabel("clientUrl", id, showVal, row);
		}
	}

	@Override
	protected Class<? extends ResourcePattern<SingleSwitchBox>> getPatternClass() {
		return SingleSwitchBoxPattern.class;
	}

	public class AirConditionerSimSimple implements RoomInsideSimulationBase {
		ResourceValueListener<BooleanResource> setPointListener = null;
		protected final BooleanResource setPoint;
		protected final BooleanResource setPointFeedback;
		
		
		@Override
		public void close() {
			if(setPointListener != null)
				setPoint.removeValueListener(setPointListener);
		}

		public AirConditionerSimSimple(BooleanResource setPoint, BooleanResource setPointFeedback,
				final ApplicationManager appMan) {
			this.setPoint = setPoint;
			this.setPointFeedback = setPointFeedback;
			setPointListener = new ResourceValueListener<BooleanResource>() {
				@Override
				public void resourceChanged(BooleanResource resource) {
					boolean value = setPoint.getValue();
					new CountDownDelayedExecutionTimer(appMan, 2000l) {
						@Override
						public void delayedExecution() {
							setPointFeedback.setValue(value);
						}
					};
				}
			};
			setPoint.addValueListener(setPointListener, true);
		}
		
	}
	@Override
	public List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, SingleSwitchBox resource,
			SingleRoomSimulationBase roomSimulation,
			DatapointService dpService) {
		List<RoomInsideSimulationBase> result = new ArrayList<>();

		result.add(new AirConditionerSimSimple(resource.onOffSwitch().stateControl(),
				resource.onOffSwitch().stateFeedback(), appMan.appMan()));
		return result;
	}
	

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
	
	@Override
	public Collection<Datapoint> getDatapoints(SingleSwitchBox dev, InstallAppDevice appDevice) {
		//FIXME Workaround
		dev.activate(true);
		//SingleSwitchBox dev = (SingleSwitchBox) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(dev.onOffSwitch().stateControl(), result, dpService);
		addDatapoint(dev.onOffSwitch().stateFeedback(), result, dpService);
		addDatapoint(dev.electricityConnection().powerSensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().energySensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().reactivePowerSensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().voltageSensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().currentSensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().frequencySensor().reading(), result, dpService);
		addDatapoint(dev.electricityConnection().reactiveAngleSensor().reading(), result, dpService);
		CommunicationStatus comStat = dev.getSubResource("communicationStatus", CommunicationStatus.class);
		if(comStat.isActive()) {
			addDatapoint(comStat.quality(), result, dpService);
			addDatapoint(comStat.getSubResource("RSSI", FloatResource.class), result, dpService);
			addDatapoint(comStat.getSubResource("Signal", FloatResource.class), result, dpService);
		}
		if(DeviceTableBase.makeDeviceToplevel(dev.getLocation()).startsWith("Tasmota")) {
			IntegerResource restartReason = dev.getSubResource("restartReason", IntegerResource.class);
			if(!restartReason.exists())
				restartReason.create().activate(false);
			addDatapoint(restartReason, result, dpService, true);
		}
		addDatapoint(dev.getSubResource("temperatureSensor", TemperatureSensor.class).reading(), result, dpService);			
		
		addtStatusDatapointsHomematic(dev, dpService, result, false);
		
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SingleSwitchBox device = (SingleSwitchBox) appDevice.device();
		boolean isShelly = DeviceTableBase.makeDeviceToplevel(device.getLocation()).startsWith("shellies");
		AlarmingUtiH.setTemplateValues(appDevice, device.onOffSwitch().stateFeedback(),
			0.0f, 1.0f, 1, isShelly?-1:AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().powerSensor().reading(),
				0.0f, 4000.0f, 10, isShelly?AlarmingUtiH.DEFAULT_NOVALUE_MINUTES:-1);
		if(isShelly) {
			AlarmingUtiH.setTemplateValues(appDevice, device.getSubResource("temperatureSensor", TemperatureSensor.class).reading(),
					0f, 55f, 10, isShelly?AlarmingUtiH.DEFAULT_NOVALUE_MINUTES:-1);			
		}
		//AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().voltageSensor().reading(),
		//		180f, 350f, 10, 20);
		//For tasmota voltage zero is sent when switch is off
		AlarmingUtiH.setAlarmingActiveStatus(appDevice, device.electricityConnection().voltageSensor().reading(), false);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().frequencySensor().reading(),
				49.8f, 50.2f, 1, AlarmingUtiH.DEFAULT_NOVALUE_MINUTES);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}
	
	@Override
	public String getInitVersion() {
		return "E";
	}

	@Override
	public String getTableTitle() {
		return "Single Switchboxes";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}

	@Override
	public SingleValueResource getMainSensorValue(SingleSwitchBox box, InstallAppDevice deviceConfiguration) {
		PowerResource power = box.electricityConnection().powerSensor().reading();
		if(power.isActive())
			return power;
		TemperatureResource temperature = box.getSubResource("temperatureSensor", TemperatureSensor.class).reading();
		if(temperature.isActive())
			return temperature;
		return box.onOffSwitch().stateFeedback();
	}
}
