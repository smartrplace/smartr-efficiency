package org.smartrplace.mqtt.devicetable;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.InstalledAppsSelector;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_Aircond extends DeviceHandlerBase<AirConditioner> {
	private final ApplicationManager appMan;
	
	public DeviceHandlerMQTT_Aircond(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Class<AirConditioner> getResourceType() {
		return AirConditioner.class;
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final AirConditioner device = (AirConditioner) addNameWidget(object, vh, id, req, row, appMan);
				Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
				Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
				Room deviceRoom = device.location().room();
				Label lastContact = addLastContact(object, vh, id, req, row, appMan, deviceRoom,  device.temperatureSensor().reading());
				addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
				if(req != null) {
					tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
					setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}
				addInstallationStatus(object, vh, id, req, row, appMan, deviceRoom);
				addComment(object, vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row, appMan, deviceRoom);
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
			protected String getTableTitle() {
				return "Air Conditioners";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<AirConditioner>> getPatternClass() {
		return AirconditionerPattern.class;
	}

	public class AirConditionerSimSimple implements RoomInsideSimulationBase {
		ResourceValueListener<TemperatureResource> setPointListener = null;
		protected final TemperatureResource setPoint;
		protected final TemperatureResource setPointFeedback;
		
		
		@Override
		public void close() {
			if(setPointListener != null)
				setPoint.removeValueListener(setPointListener);
		}

		public AirConditionerSimSimple(TemperatureResource setPoint, TemperatureResource setPointFeedback,
				final ApplicationManager appMan) {
			this.setPoint = setPoint;
			this.setPointFeedback = setPointFeedback;
			setPointListener = new ResourceValueListener<TemperatureResource>() {
				@Override
				public void resourceChanged(TemperatureResource resource) {
					float value = setPoint.getValue();
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
	public	RoomInsideSimulationBase startSimulationForDevice(AirConditioner resource,
			SingleRoomSimulationBase roomSimulation,
			DatapointService dpService) {
		return new AirConditionerSimSimple(resource.temperatureSensor().settings().setpoint(),
				resource.temperatureSensor().deviceFeedback().setpoint(), appMan);
	}
	
	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
}
