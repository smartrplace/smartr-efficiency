package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
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
public class DeviceHandlerMQTT_ElecConnBox extends DeviceHandlerBase<ElectricityConnectionBox> {
	private final ApplicationManager appMan;
	
	public DeviceHandlerMQTT_ElecConnBox(ApplicationManager appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final ElectricityConnectionBox box =
						(ElectricityConnectionBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();
				Label lastContact = null;

				//for (ElectricityConnection c : box.getSubResources(ElectricityConnection.class, true)) {
				// FIXME: show _all_ connections
				{   ElectricityConnection c = box.connection();
					Label voltage = vh.floatLabel("Voltage (" + ResourceUtils.getHumanReadableShortName(c) + ")",
							id, c.voltageSensor().reading(), row, "%.1f");
					Label power = vh.floatLabel("Power (" + ResourceUtils.getHumanReadableShortName(c) + ")",
							id, c.powerSensor().reading(), row, "%.1f");
					if (req != null) {
						voltage.setPollingInterval(DEFAULT_POLL_RATE, req);
						power.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
					if (lastContact == null) {
						lastContact = addLastContact(object, vh, id, req, row, appMan, deviceRoom,
								c.voltageSensor().reading());
					}
				}

				addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
				addInstallationStatus(object, vh, id, req, row, appMan, deviceRoom);
				addComment(object, vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row, appMan, deviceRoom);

			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_ElecConnBox.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_ElecConnBox.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Electricity Meters";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return ElectricityConnectionBoxPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(ElectricityConnectionBox dev, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(dev.connection().voltageSensor().reading(), result, dpService);
		addDatapoint(dev.connection().powerSensor().reading(), result, dpService);
		addDatapoint(dev.connection().energySensor().reading(), result, dpService);
		addDatapoint(dev.connection().currentSensor().reading(), result, dpService);
		addDatapoint(dev.connection().frequencySensor().reading(), result, dpService);
		return result;
	}
}