package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.model.sensors.DoorWindowSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.expert.HardwareInstallControllerExpert;
import org.smartrplace.apps.hw.install.gui.DoorWindowSensorTable;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class DoorWindowSensorTableExpert extends DoorWindowSensorTable {

	public DoorWindowSensorTableExpert(WidgetPage<?> page, HardwareInstallControllerExpert controller,
			InstalledAppsSelector instAppSelector, Alert alert) {
			//RoomSelectorDropdown roomsDrop, Alert alert) {
		super(page, controller, instAppSelector, alert);
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		// TODO
		//DoorWindowSensor device = super.addWidgetsInternal(object, vh, id, req, row, appMan);
		//addWidgetsCommonExpert(object, vh, id, req, row, appMan, device.location().room());
	}
}