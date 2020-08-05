package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {
	
	@Override
	protected String getHeader() {return "Smartrplace Hardware InstallationApp Expert";}

	public MainPageExpert(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller);
	}

	@Override
	public void addWidgetsExpert(final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		vh.stringLabel("IAD", id, object.getName(), row);
		vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		if(req == null) {
			vh.registerHeaderEntry("Delete");
			vh.registerHeaderEntry("Reset");
			return;
		}
		ButtonConfirm deleteButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				object.device().getLocationResource().delete();
				object.delete();
			}
		};
		deleteButton.setDefaultConfirmMsg("Really delete "+object.device().getLocation()+" ?");
		deleteButton.setDefaultText("Delete");
		row.addCell("Delete", deleteButton);
		ButtonConfirm resetButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("resetBut"+id), req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				object.delete();
			}
		};
		resetButton.setDefaultConfirmMsg("Really delete installation&setup configuration for "+object.device().getLocation()+" ? Search for new devices to recreate clean configuration.");
		resetButton.setDefaultText("Reset");
		row.addCell("Reset", resetButton);
	}
	
	/*@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//Thermostat device = super.addWidgetsInternal(object, vh, id, req, row, appMan);
		//vh.booleanEdit("Bang", id, device.getSubResource("bangBangControlActive", BooleanResource.class), row);
		//addWidgetsCommonExpert(object, vh, id, req, row, appMan, device.location().room());
	}*/
	
}	
