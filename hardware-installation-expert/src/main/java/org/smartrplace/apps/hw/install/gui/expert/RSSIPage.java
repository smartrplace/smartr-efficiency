package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.expert.HardwareInstallControllerExpert;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class RSSIPage extends MainPage {
	
	@Override
	protected String getHeader() {return "Smartrplace Communication Quality Supervision Page";}

	public RSSIPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller);
	}


	@Override
	protected void finishConstructor() {
		//DoorWindowSensorTableRSSI winSensTable = new DoorWindowSensorTableRSSI(page,
		//		(HardwareInstallControllerExpert) controller, this, alert);
		//winSensTable.triggerPageBuild();
		triggerPageBuild();		
	}

	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		/*
		Thermostat device = super.addWidgetsInternal(object, vh, id, req, row, appMan);
		Room deviceRoom = device.location().room();
		//vh.booleanEdit("Bang", id, device.getSubResource("bangBangControlActive", BooleanResource.class), row);
		addWidgetsCommonExpert(object, vh, id, req, row, appMan, deviceRoom);
		Resource hmRes = ResourceHelper.getFirstParentOfType(device, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice");
		addDeleteButton(vh, id, req, row, appMan, deviceRoom, hmRes);
		*/
	}
}
