package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.communication.CommunicationStatus;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.locations.Room;
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
public class DeviceHandlerMQTT_SingleSwBox extends DeviceHandlerBase<SingleSwitchBox> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerMQTT_SingleSwBox(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<SingleSwitchBox> getResourceType() {
		return SingleSwitchBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final SingleSwitchBox box = (SingleSwitchBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();

				//int nSwitchboxes = box.switchboxes().size();
				//Label switchboxCount = vh.stringLabel("Switchbox Count", id, Integer.toString(nSwitchboxes), row);

				Label stateFB = vh.booleanLabel("StateFB", id, box.onOffSwitch().stateFeedback(), row, 0);
				Label lastFB = addLastContact("Last FB", vh, "LFB"+id, req, row, box.onOffSwitch().stateFeedback());
				vh.booleanEdit("Control", id, box.onOffSwitch().stateControl(), row);
				Label power = vh.floatLabel("Power", id, box.electricityConnection().powerSensor().reading(), row, "%.1f");
				Label lastPower = addLastContact("Last Power", vh, "LPower"+id, req, row, box.electricityConnection().powerSensor().reading());
				
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
				
				if(stateFB != null)
					stateFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				if(power != null)
					power.setDefaultPollingInterval(DEFAULT_POLL_RATE);

				if(lastFB != null)
					lastFB.setDefaultPollingInterval(DEFAULT_POLL_RATE);
				if(lastPower != null)
					lastPower.setDefaultPollingInterval(DEFAULT_POLL_RATE);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_SingleSwBox.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_SingleSwBox.this.id();
			}

			@Override
			public String getTableTitle() {
				return "Single Switchboxes";
			}
		};
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
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		SingleSwitchBox dev = (SingleSwitchBox) appDevice.device();
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
		return result;
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SingleSwitchBox device = (SingleSwitchBox) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.onOffSwitch().stateFeedback(),
			0.0f, 1.0f, 1, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().powerSensor().reading(),
				0.0f, 4000.0f, 10, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().voltageSensor().reading(),
				180f, 350f, 10, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.electricityConnection().frequencySensor().reading(),
				49.8f, 50.2f, 1, 20);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}
}
