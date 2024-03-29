package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smatrplace.apps.hw.install.gui.mainexpert.MainPageExpert;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class MainPageExpertTrash extends MainPageExpert {

	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		return true;
	}
	
	public MainPageExpertTrash(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller, true, ShowModeHw.STANDARD);
		
		ButtonConfirm clearDeviceMarkedForDeletetion = new ButtonConfirm(page, "clearDeviceMarkedForDeletetion",
				"Delete Devices Marked") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Collection<InstallAppDevice> all = controller.getDevices(null, true);
				for(InstallAppDevice dev: all) {
					if((!dev.isTrash().getValue()) || (dev.trashStatus().getValue() <= 0))
						continue;
					DeviceTableRaw.deleteDevice(dev, controller.hwInstApp.gwSync);
				}
			}
		};
		clearDeviceMarkedForDeletetion.setDefaultConfirmMsg("Really delete all devices marked and also trigger deletion from CCU if applicable?");
		topTable.setContent(0, 4, clearDeviceMarkedForDeletetion);
	}
	
	@Override
	public String getHeader() {
		return "Trash-marked Devices";
	}

	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		super.addWidgetsExpert(tableProvider, object, vh, id, req, row, appMan);
		vh.intLabel("ToDelete", id, object.trashStatus(), row, 0);
	}
	
	@Override
	public List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req) {
		//List<InstallAppDevice> all = controller.getDevices(devHand, true, true);
		Collection<InstallAppDevice> all = controller.getDevices(devHand, true);
		/*all = roomsDrop.getDevicesSelected(all, req);
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all, req);*/
		List<InstallAppDevice> allTrash = new ArrayList<>();
		for(InstallAppDevice dev: all) {
			if((!dev.device().exists()) && dev.trashStatus().getValue() >= 1) {
				//workaround for error of re-creation
				dev.delete();
			}
			if(dev.isTrash().getValue())
				allTrash.add(dev);
		}
		return finalFilter.getFiltered(allTrash, req);
		//return allTrash;
	}
	
	@Override
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really get back "+object.device().getLocation()+" from trash and re-initialize?";
	}

	@Override
	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProviderDP<?> devHand) {
		for(Resource dev: devHand.devicesControlled(object)) {
			if(controller.hwInstApp.gwSync != null)
				controller.hwInstApp.gwSync.activateResource(dev.getLocationResource(), true);
			else
				dev.getLocationResource().activate(true);
		}
		//object.device().getLocationResource().activate(true);
		object.isTrash().setValue(false);
		if(object.trashStatus().exists()) {
			object.trashStatus().setValue(0);
			object.trashStatus().delete();
		}
		//re-init alarming and datapoints in general
		controller.updateDatapoints(devHand, object);
	}
}
