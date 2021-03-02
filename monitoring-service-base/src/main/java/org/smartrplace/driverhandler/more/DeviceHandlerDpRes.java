package org.smartrplace.driverhandler.more;

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
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.dpres.SensorDeviceDpRes;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerDpRes extends DeviceHandlerBase<SensorDeviceDpRes> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerDpRes(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<SensorDeviceDpRes> getResourceType() {
		return SensorDeviceDpRes.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final SensorDeviceDpRes box =
						(SensorDeviceDpRes) addNameWidget(object, vh, id, req, row, appMan);
				GenericFloatSensor sampleSensor = null;
				FloatResource reading = null;
				for(GenericFloatSensor sens: box.sensors().getAllElements()) {
					sampleSensor = sens;
					reading = sens.reading();
					break;
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
				return DeviceHandlerDpRes.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerDpRes.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Virtual Calendar Devices";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDeviceDpRes>> getPatternClass() {
		return SensorDeviceDpResPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice appDevice, DatapointService dpService) {
		SensorDeviceDpRes dev = (SensorDeviceDpRes) appDevice.device();
		List<Datapoint> result = new ArrayList<>();
		for(Sensor sens: dev.sensors().getAllElements()) {
			//String loc = sens.getName();
			//Datapoint dp = dpService.getDataPointAsIs(loc);
			//if(dp != null)
			//	result.add(dp);
			if(sens.reading() instanceof SingleValueResource) {
				String name = sens.getName();
				int idx = name.indexOf("$$");
				if(idx > 0) {
					name = name.substring(0, idx);
					if(name.equals("NEXT"))
						name = "NextEventChange";
				}
				addDatapoint((SingleValueResource) sens.reading(), result, name, dpService);
			}
		}
		return result;
	}
}
