package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;

public class MainPageExpertTrash extends MainPageExpert {

	public MainPageExpertTrash(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller);
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		List<InstallAppDevice> allNonTrash = new ArrayList<>();
		for(InstallAppDevice dev: all) {
			if(dev.isTrash().getValue())
				allNonTrash.add(dev);
		}
		return all;
	}
	
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really get back "+object.device().getLocation()+" from trash and re-initialize?";
	}

	@Override
	protected void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		object.device().getLocationResource().activate(true);
		object.isTrash().setValue(false);
		//re-init alarming and datapoints in general
		controller.updateDatapoints(devHand, object);
	}
}
