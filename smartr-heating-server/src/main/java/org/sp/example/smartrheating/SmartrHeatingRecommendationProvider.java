package org.sp.example.smartrheating;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProposalProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class SmartrHeatingRecommendationProvider implements ProposalProvider {

	@Override
	public String id() {
		return SmartrHeatingRecommendationProvider.class.getSimpleName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Recommendations";
	}

	@Override
	public String description(OgemaLocale locale) {
		return label(locale);
	}

	/*@Override
	public List<Class<? extends SmartEffResource>> inputResourceTypes() {
		List<Class<? extends SmartEffResource>> result = new ArrayList<>();
		result .add(BuildingData.class);
		result.add(SmartrHeatingData.class);
		return result;
	}*/

	/*@Override
	public void updateRecommendations(SmartEffUserData userData, SmartEffGeneralData generalData,
			List<SmartEffResource> resourcesChanged, List<Recommendation> recommendations) {
		final List<BuildingData> buildings;
		if(resourcesChanged == null) {
			buildings = userData.buildings().getAllElements();
		} else
			buildings = getBuildingsChanged(resourcesChanged);
		for(BuildingData b: buildings) {
			SrtrHeatingRecommendation rec = checkBuilding(b);
			for(Recommendation exist: recommendations) {
				if(((SrtrHeatingRecommendation)exist).building.equalsLocation(b)) {
					recommendations.remove(exist);
					if(rec != null) recommendations.add(rec);
					break;
				}
			}
		}
	}*/
	
	/*private List<BuildingData> getBuildingsChanged(List<SmartEffResource> resourcesChanged) {
		//TODO
		return null;
	}
	private SrtrHeatingRecommendation checkBuilding(BuildingData building) {
		//TODO
		return null;
	}*/

	public List<EntryType> getEntryTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Resource> calculate(ExtensionResourceAccessInitData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<CalculationResultType> resultTypes() {
		// TODO Auto-generated method stub
		return null;
	}
}
