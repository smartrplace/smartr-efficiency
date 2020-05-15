package org.smartrplace.mqtt.devicetable;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableBase.InstalledAppsSelector;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.sensoractordevices.MultiSwitchBox;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


@Component(specVersion = "1.2", immediate = true)
@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_MultiSwBox extends DeviceHandlerBase<MultiSwitchBox> {
	@Override
	public Class<MultiSwitchBox> getResourceType() {
		return MultiSwitchBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final MultiSwitchBox box = (MultiSwitchBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();

				int nSwitchboxes = box.switchboxes().size();
				Label switchboxCount = vh.stringLabel("Switchbox Count", id, Integer.toString(nSwitchboxes), row);

				addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
				addInstallationStatus(object, vh, id, req, row, appMan, deviceRoom);
				addComment(object, vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row, appMan, deviceRoom);

			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_MultiSwBox.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_MultiSwBox.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Multi Switchboxes";
			}
		};
	}

	@Override
	protected Class<? extends ResourcePattern<MultiSwitchBox>> getPatternClass() {
		return MultiSwitchBoxPattern.class;
	}

}
