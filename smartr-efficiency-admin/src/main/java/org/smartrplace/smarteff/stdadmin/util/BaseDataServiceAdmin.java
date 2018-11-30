package org.smartrplace.smarteff.stdadmin.util;

import java.util.Arrays;
import java.util.Collection;

import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.timeseries.GenericDriverProvider;

//@Service(SmartEffExtensionService.class)
//@Component
public class BaseDataServiceAdmin implements SmartEffExtensionService {
	public GenericDriverProvider GENERIC_DRIVER_PROVIDER;
	
	public BaseDataServiceAdmin(SpEffAdminController controller) {
		 GENERIC_DRIVER_PROVIDER = controller.tsDriver;
	}

	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {GENERIC_DRIVER_PROVIDER});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		return null;
	}
}
