package org.sp.example.smarteff.eval.capability;

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
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.sp.example.buildingwizard.WizBexBuildingEditPage;
import org.sp.example.buildingwizard.WizBexEntryPage;
import org.sp.example.buildingwizard.WizBexRoomEditPage;
import org.sp.example.smarteff.roomext.RoomLightingRegistration;
import org.sp.example.smarteff.roomext.RoomLightingRegistration.EditPage;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.basic.evals.BuildingControlQualityFeedback;
import extensionmodel.smarteff.basic.evals.BuildingEvalData;

@Service(SmartEffExtensionService.class)
@Component
public class SPEvalDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	/*public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	*/
	public BuildingPresenceEval BUILDING_PRESENCE_PROVIDER;
	static final NaviPageBase<BuildingEvalData>.Provider PARAM_PAGE = new BuildingEvalParamsPage().provider;
	static final WizBexEntryPage WIZBEX_ENTRY = new WizBexEntryPage();
	public static final WizBexBuildingEditPage WIZBEX_BUILDING = new WizBexBuildingEditPage();
	public static final WizBexRoomEditPage WIZBEX_ROOM = new WizBexRoomEditPage();
	static final EditPage ROOMLIGHT_PAGE = new RoomLightingRegistration.EditPage();

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
		return Arrays.asList(new ExtensionCapability[] {BUILDING_PRESENCE_PROVIDER, PARAM_PAGE,
				ROOMLIGHT_PAGE.provider,
				WIZBEX_ENTRY.provider, WIZBEX_BUILDING.provider, WIZBEX_ROOM.provider});
	}

	public static final ExtensionResourceTypeDeclaration<SmartEffResource> QUALITY_FB_TYPE = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends BuildingControlQualityFeedback> dataType() {
			return BuildingControlQualityFeedback.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Building Control Quality Feedback Extension";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_OPTIONAL;
		}
		
	};
	
	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		if(BUILDING_PRESENCE_PROVIDER.getParamTypeDeclaration() != null) result.add(BUILDING_PRESENCE_PROVIDER.getParamTypeDeclaration());
		result.add(new RoomLightingRegistration.TypeDeclaration());
		result.add(QUALITY_FB_TYPE);
		//dependencies
		result.add(BaseDataService.BUILDING_DATA);
		result.add(BaseDataService.ROOM_TYPE);
		result.add(BaseDataService.RADIATOR_TYPE);
		return result ;
	}
}
