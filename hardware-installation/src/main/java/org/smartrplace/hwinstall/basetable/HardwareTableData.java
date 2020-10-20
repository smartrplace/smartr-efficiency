package org.smartrplace.hwinstall.basetable;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.gui.RoomSelectorDropdown;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;

public class HardwareTableData {
	public final HardwareInstallConfig appConfigData;
	public final AccessAdminConfig accessAdminConfigRes;
	
	public HardwareTableData(ApplicationManager appMan) {
		accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		appConfigData = appMan.getResourceAccess().getResource("hardwareInstallConfig");
	}
}
