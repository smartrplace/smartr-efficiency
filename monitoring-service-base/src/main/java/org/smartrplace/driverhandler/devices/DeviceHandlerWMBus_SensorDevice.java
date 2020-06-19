package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerWMBus_SensorDevice extends DeviceHandlerBase<SensorDevice> {
	private final ApplicationManager appMan;
	
	public DeviceHandlerWMBus_SensorDevice(ApplicationManager appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final SensorDevice box =
						(SensorDevice) addNameWidget(object, vh, id, req, row, appMan);
				Sensor sampleSensor = null;
				FloatResource reading = null;
				for(Sensor sens: box.sensors().getAllElements()) {
					if(sens.reading() instanceof FloatResource) {
						sampleSensor = sens;
						reading = (FloatResource) sens.reading();
						break;
					}
				}
				
				Room deviceRoom = box.location().room();

				if(sampleSensor != null) {
					Label value = vh.floatLabel("Sensor value (" + ResourceUtils.getHumanReadableShortName(sampleSensor) + ")",
								id, reading, row, "%.1f");
					Label lastContact = addLastContact(object, vh, id, req, row, appMan, deviceRoom, reading);
					if (req != null) {
						value.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
				addInstallationStatus(object, vh, id, req, row, appMan, deviceRoom);
				addComment(object, vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row, appMan, deviceRoom);

			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerWMBus_SensorDevice.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerWMBus_SensorDevice.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Sensor Devices";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return SensorDevicePattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		SensorDevice dev = (SensorDevice) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		for(Sensor sens: dev.sensors().getAllElements()) {
			if(sens.reading() instanceof SingleValueResource)
				addDatapoint((SingleValueResource) sens.reading(), result, dpService);			
		}
		return result;
	}
}