package org.sp.example.smartrheating;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.capabilities.SmartEffRecommendationProvider.Recommendation;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;

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

	private class SrtrHeatingRecommendation implements Recommendation {
		public BuildingData building;
		
		@Override
		public String id() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String label(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String description(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public float getNetInvestment() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getAnnualNetSavings() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getAnnualCO2Savings() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
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
