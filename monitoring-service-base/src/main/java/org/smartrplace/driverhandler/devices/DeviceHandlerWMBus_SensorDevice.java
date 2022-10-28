package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
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
public class DeviceHandlerWMBus_SensorDevice extends DeviceHandlerSimple<SensorDevice> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerWMBus_SensorDevice(ApplicationManagerPlus appMan) {
		super(appMan, true);
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
					Label lastContact = addLastContact(vh, id, req, row, reading);
					if (req != null) {
						value.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerWMBus_SensorDevice.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerWMBus_SensorDevice.this.id();
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return SensorDeviceGenericPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(SensorDevice dev, InstallAppDevice appDevice) {
		//SensorDevice dev = (SensorDevice) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		boolean addElementName = dev.sensors().size() > 1;
		boolean isJmbus = dev.getLocation().toLowerCase().contains("jmbus") || dev.getLocation().toLowerCase().contains("JMBUS_BASE");
		String sensorDevName = null;
		if(!isJmbus)
			sensorDevName = dev.getLocationResource().getName();
		for(Sensor sens: dev.sensors().getAllElements()) {
			if(sens.reading() instanceof SingleValueResource) {
				if(isJmbus)
					addDatapoint((SingleValueResource) sens.reading(), result, dpService);
				else if(addElementName) {
					String elName;
					//if(sens.isReference(false))
					//	elName= sens.getLocationResource().getName();
					//else
					elName= sens.getName();
					addDatapoint((SingleValueResource) sens.reading(), result,
							sensorDevName+"-"+elName, dpService);
				} else
					addDatapoint((SingleValueResource) sens.reading(), result, sensorDevName, dpService);
			}
		}
		return result;
	}
	
	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "WMBS";
	}

	@Override
	public String getTableTitle() {
		return "Sensor Devices";
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice box, InstallAppDevice deviceConfiguration) {
		for(Sensor sens: box.sensors().getAllElements()) {
			if(sens.reading() instanceof FloatResource) {
				return (FloatResource)sens.reading();
			}
		}
		return null;
	}
}


