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
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.ogema.bacnet.models.BACnetDevice;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class BacnetDeviceHandler extends DeviceHandlerBase<BACnetDevice> {

	private final ApplicationManagerPlus appMan;

	public BacnetDeviceHandler(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerBacnet";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return BacnetDeviceHandler.this.getResourceType();
			}

			@Override
			protected String id() {
				return BacnetDeviceHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "BACnet Devices";
			}
			
			public BACnetDevice addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final BACnetDevice device;
				if(req == null)
					device = ResourceHelper.getSampleResource(BACnetDevice.class);
				else
					device = (BACnetDevice) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				if(req == null) {
					vh.registerHeaderEntry("SampleValue");
					vh.registerHeaderEntry("Last Value");
					vh.registerHeaderEntry("Room");
					vh.registerHeaderEntry("Location");
				} else {
					List<Resource> objects = device.objects().getAllElements();
					FloatResource sampleRes = null;
					for(Resource obj: objects) {
						if(obj instanceof Sensor && (((Sensor)obj).reading() instanceof FloatResource)) {
							sampleRes = (FloatResource) ((Sensor)obj).reading();
							break;
						} else {
							for(Sensor sens: obj.getSubResources(Sensor.class, true)) {
								if(sens instanceof Sensor && (((Sensor)sens).reading() instanceof FloatResource)) {
									sampleRes = (FloatResource) ((Sensor)sens).reading();
									break;
								}
							}
							if(sampleRes != null)
								break;
						}
					}
					if(sampleRes != null && sampleRes.exists()) {
						Label tempmes = vh.floatLabel("SampleValue", id, sampleRes, row, "%.1f");
						Label lastContact = addLastContact("Last Value", vh, id, req, row, sampleRes);
						tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
					Room deviceRoom = device.location().room();
					addRoomWidget(vh, id, req, row, appMan, deviceRoom);
					addSubLocation(object, vh, id, req, row);
				}
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				
				return device;
			}
			
		};
	}

	@Override
	public Class<BACnetDevice> getResourceType() {
		return BACnetDevice.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		BACnetDevice device = (BACnetDevice) installDeviceRes.device();
		List<Resource> objects = device.objects().getAllElements();
		for(Resource obj: objects) {
			if(obj instanceof Sensor && (((Sensor)obj).reading() instanceof SingleValueResource)) {
				result.add(dpService.getDataPointStandard(((Sensor)obj).reading()));
			} else 	if(obj instanceof OnOffSwitch) {
				result.add(dpService.getDataPointStandard(((OnOffSwitch)obj).stateControl()));
				result.add(dpService.getDataPointStandard(((OnOffSwitch)obj).stateFeedback()));
			} else 	if(obj instanceof MultiSwitch) {
				result.add(dpService.getDataPointStandard(((MultiSwitch)obj).stateControl()));
				result.add(dpService.getDataPointStandard(((MultiSwitch)obj).stateFeedback()));
			}

		}
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<BACnetDevice>> getPatternClass() {
		return BacnetDevicePattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
}
