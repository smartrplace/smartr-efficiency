package org.sp.example.smarteff.eval.capability;

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
	public BuildingPresenceEval BUILDING_PRESENCE_PROVIDER;
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		BUILDING_PRESENCE_PROVIDER = new BuildingPresenceEval(appManExt);
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
		return null;
	}

	/*@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(PRICE_DATA);
		result.add(BUILDINGANALYSIS_PROVIDER.getTypeDeclaration());
		if(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration() != null) result.add(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration());
		result.add(new MasterUserRegistration.TypeDeclaration());
		result.add(new RoomRegistration.TypeDeclaration());
		result.add(new HeatBillRegistration.TypeDeclaration());
		return result ;
	}*/
}
