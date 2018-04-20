package org.smartrplace.smarteff.admin.object;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.efficiency.api.capabilities.SmartEffRecommendationProvider;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.SpEffAdminController;

public class SmartrEffExtResourceTypeData {
	public final Class<? extends Resource> resType;
	public final ExtensionResourceTypeDeclaration<? extends Resource> typeDeclaration;
	public final List<SmartEffExtensionService> requiredBy = new ArrayList<>();
	//public int numberTotal;
	//public int numberPublic;
	//public int numberNonEdit;
	
	public SmartrEffExtResourceTypeData(ExtensionResourceTypeDeclaration<? extends SmartEffResource> typeDeclaration,
			SmartEffExtensionService parent, SpEffAdminController app) {
		this.resType = typeDeclaration.dataType();
		this.typeDeclaration = typeDeclaration;
		addParent(parent);
		if(app != null) resetResourceStatistics(app);
	}
	
	public void resetResourceStatistics(SpEffAdminController app) {
		//numberTotal = app.appMan.getResourceAccess().getResources(resType).size();
		//numberPublic = app.getUserAdmin().getAppConfigData().globalData().getSubResources(resType, true).size();
		//numberNonEdit = app.getUserAdmin().getAllUserResource().getSubResources(resType, true).size();		
	}

	public void addParent(SmartEffExtensionService parent) {
		this.requiredBy.add(parent);
	}
	
	/** 
	 * 
	 * @param parent
	 * @return true if the type is not used anymore
	 */
	public boolean removeParent(SmartEffExtensionService parent) {
		this.requiredBy.remove(parent);
		if(requiredBy.isEmpty()) return true;
		else return false;
	}
	
	public static class ServiceCapabilities {
		public final Set<ProposalProvider> proposalProviders = new LinkedHashSet<>();
		public final Set<NavigationGUIProvider> naviProviders = new LinkedHashSet<>();
		public final Set<SmartEffRecommendationProvider> recommendationProviders = new LinkedHashSet<>();
		public final Set<ExtensionCapability> otherProviders = new LinkedHashSet<>();
	}
	public static ServiceCapabilities getServiceCaps(SmartEffExtensionService service) {
		ServiceCapabilities result = new ServiceCapabilities();
    	for(ExtensionCapability c: service.getCapabilities()) {
    		if(c instanceof ProposalProvider) result.proposalProviders.add((ProposalProvider) c);
    		else if(c instanceof SmartEffRecommendationProvider) result.recommendationProviders.add((SmartEffRecommendationProvider) c);
    		else if(c instanceof NavigationGUIProvider) result.naviProviders.add((NavigationGUIProvider) c);
    		else result.otherProviders.add(c);
    	}
		return result;
	}

	@SuppressWarnings("incomplete-switch")
	public void registerElement(Resource res) {
		/*numberTotal++;
		switch(SmartrEffUtil.getAccessType(res)) {
		case PUBLIC:
			numberPublic++;
			break;
		case READONLY:
			numberNonEdit++;
			break;
		}*/		
	}
	@SuppressWarnings("incomplete-switch")
	public void unregisterElement(SmartEffResource res) {
		/*numberTotal--;
		switch(SmartrEffUtil.getAccessType(res)) {
		case PUBLIC:
			numberPublic--;
			break;
		case READONLY:
			numberNonEdit--;
			break;
		}*/		
	}
	
	@Override
	public String toString() {
		return ResourceUtils.getValidResourceName(resType.getName());
	}
}
