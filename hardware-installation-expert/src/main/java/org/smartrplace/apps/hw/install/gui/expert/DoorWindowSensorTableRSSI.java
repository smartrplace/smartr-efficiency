package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.DoorWindowSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.expert.HardwareInstallControllerExpert;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

public class DoorWindowSensorTableRSSI extends DoorWindowSensorTableExpert {

	public DoorWindowSensorTableRSSI(WidgetPage<?> page, HardwareInstallControllerExpert controller,
			InstalledAppsSelector instAppsSelector, Alert alert) {
			//RoomSelectorDropdown roomsDrop, Alert alert) {
		super(page, controller, instAppsSelector, alert);
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		/*
		TODO
		DoorWindowSensor device = addWidgetsInternal(object, vh, id, req, row, appMan);
		IntegerResource rssiRes = addWidgetsCommonExpert(object, vh, id, req, row, appMan, device.location().room());
		
		Label lastRSSI = null;
		if(req != null) {
			lastRSSI = new LastContactLabel(rssiRes, appMan, mainTable, "lastRSSI"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last RSSI"), lastRSSI);
			lastRSSI.setPollingInterval(DEFAULT_POLL_RATE, req);
		} else
			vh.registerHeaderEntry("Last RSSI");

		Resource hmRes = ResourceHelper.getFirstParentOfType(object.device(), "org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice");
		addDeleteButton(vh, id, req, row, appMan, device.location().room(), hmRes);
		 */

	}
	
	@Override
	public DoorWindowSensor addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		/*
		TODO
		final DoorWindowSensor device = addNameWidget(object, vh, id, req, row, appMan);
		Label state = vh.booleanLabel("Measured State", id, device.reading(), row, 0);
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(device.reading(), appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
		} else
			vh.registerHeaderEntry("Last Contact");
		addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
		
		if(req != null) {
			state.setPollingInterval(DEFAULT_POLL_RATE, req);
			lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
		}
		return device;
		*/
		return null;
	}

	@Override
	protected void addWidgetsCommon(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);
		
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
		if(req != null) {
			String text = getHomematicCCUId(object.device().getLocation());
			vh.stringLabel("RT", id, text, row);
		} else
			vh.registerHeaderEntry("RT");
	}
}