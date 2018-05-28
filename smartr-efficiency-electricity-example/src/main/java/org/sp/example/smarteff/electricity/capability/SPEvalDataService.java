package org.sp.example.smarteff.electricity.capability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

@Service(SmartEffExtensionService.class)
@Component
public class SPEvalDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	/*public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	*/
	public ElectricityProfileEval BUILDING_PRESENCE_PROVIDER;
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		BUILDING_PRESENCE_PROVIDER = new ElectricityProfileEval(appManExt);
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {BUILDING_PRESENCE_PROVIDER});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		if(BUILDING_PRESENCE_PROVIDER.getParamTypeDeclaration() != null) result.add(BUILDING_PRESENCE_PROVIDER.getParamTypeDeclaration());
		//result.add(new HeatBillRegistration.TypeDeclaration());
		return result ;
	}
}
