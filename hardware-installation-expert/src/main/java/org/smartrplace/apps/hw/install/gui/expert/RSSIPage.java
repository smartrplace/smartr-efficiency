package org.smartrplace.apps.hw.install.gui.expert;

import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.gui.MainPage;

import de.iwes.widgets.api.widgets.WidgetPage;

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
		//triggerPageBuild();		
	}

	/*@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
	}*/
}
