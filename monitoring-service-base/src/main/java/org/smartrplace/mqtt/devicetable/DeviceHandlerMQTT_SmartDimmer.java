package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.buildingtechnology.ElectricLight;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.LightSensor;
import org.ogema.model.sensors.MotionSensor;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_SmartDimmer extends DeviceHandlerBase<SensorDevice> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerMQTT_SmartDimmer(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final SensorDevice box = (SensorDevice) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();

				List<ElectricLight> dimmers = box.getSubResources(ElectricLight.class, false);
				int nSwitchboxes = dimmers.size();
				vh.stringLabel("Dimmer Count", id, Integer.toString(nSwitchboxes), row);
				
				if(!dimmers.isEmpty()) {
					Label stateFB = vh.floatLabel("StateFB0", id, dimmers.get(0).setting().stateFeedback(), row, "%.1f");
					Label lastFB = addLastContact("Last FB0", vh, "LFB"+id, req, row, dimmers.get(0).setting().stateFeedback());
					vh.floatEdit("Control0", id, dimmers.get(0).setting().stateControl(), row, alert, 0.0f, 1.0f, "Dimmer control must be between 0.0 and 1.0!");
					if(stateFB != null)
						stateFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);

					if(lastFB != null)
						lastFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				} else if(req == null) {
					vh.registerHeaderEntry("StateFB0");
					vh.registerHeaderEntry("Last FB0");
					vh.registerHeaderEntry("Control0");
				}
				Label power = vh.floatLabel("Power", id, box.electricityConnection().powerSensor().reading(), row, "%.1f");
				Label lastPower = addLastContact("Last Power", vh, "LPower"+id, req, row, box.electricityConnection().powerSensor().reading());
				if(power != null)
					power.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				if(lastPower != null)
					lastPower.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(DeviceHandlerMQTT_SmartDimmer.this, object, vh, id, req, row, appMan);
				
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_SmartDimmer.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_SmartDimmer.this.id();
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return SmartDimmerMQTTPattern.class;
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
	public List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, SensorDevice resource,
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
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		SensorDevice maindev = (SensorDevice) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		for(ElectricLight dev: maindev.getSubResources(ElectricLight.class, false)) {
			addDatapoint(dev.setting().stateControl(), result, dev.getName(), dpService);
			addDatapoint(dev.setting().stateFeedback(), result, dev.getName(), dpService);			
		}
		addDatapoint(maindev.electricityConnection().powerSensor().reading(), result, dpService);
		LightSensor dev = maindev.getSubResource("lightSensor", LightSensor.class);
		addDatapoint(dev.reading(), result, dpService);
		MotionSensor devM = maindev.getSubResource("motionSensor", MotionSensor.class);
		addDatapoint(devM.reading(), result, dpService);
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SensorDevice maindev = (SensorDevice ) appDevice.device();
		for(ElectricLight dev: maindev.getSubResources(ElectricLight.class, false)) {
			AlarmingUtiH.setTemplateValues(appDevice, dev.setting().stateFeedback(), 0.0f, 1.0f, 1, 20);			
		}
		AlarmingUtiH.setTemplateValues(appDevice, maindev.electricityConnection().powerSensor().reading(),
				0.0f, 4000.0f, 10, 20);
		AlarmingUtiH.addAlarmingMQTT(maindev, appDevice);
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "SDIM";
	}

	@Override
	public String getTableTitle() {
		return "Smart Dimmers";
	}
}
