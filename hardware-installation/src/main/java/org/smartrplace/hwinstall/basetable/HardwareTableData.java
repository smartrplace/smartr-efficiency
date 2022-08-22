package org.smartrplace.hwinstall.basetable;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;

public class HardwareTableData {
	public HardwareInstallConfig appConfigData;
	public final AccessAdminConfig accessAdminConfigRes;
	
	public HardwareTableData(ApplicationManager appMan) {
		accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		appConfigData = appMan.getResourceAccess().getResource("hardwareInstallConfig");
	}
}
