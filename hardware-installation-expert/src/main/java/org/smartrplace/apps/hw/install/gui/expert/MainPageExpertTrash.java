package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class MainPageExpertTrash extends MainPageExpert {

	public MainPageExpertTrash(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, true);
	}
	
	@Override
	protected String getHeader() {
		return "Trash-marked Devices";
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req) {
		//List<InstallAppDevice> all = controller.getDevices(devHand, true, true);
		List<InstallAppDevice> all = controller.getDevices(devHand, true);
		/*all = roomsDrop.getDevicesSelected(all, req);
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all, req);*/
		List<InstallAppDevice> allTrash = new ArrayList<>();
		for(InstallAppDevice dev: all) {
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
	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		object.device().getLocationResource().activate(true);
		object.isTrash().setValue(false);
		//re-init alarming and datapoints in general
		controller.updateDatapoints(devHand, object);
	}
}
