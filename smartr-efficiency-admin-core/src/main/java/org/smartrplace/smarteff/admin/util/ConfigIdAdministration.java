package org.smartrplace.smarteff.admin.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData.ConfigInfo;

/** TODO: This is not thread-safe yet
 * TODO: This has no clean-up mechanism yet
 * TODO: Generate configId randomly individually*/
public class ConfigIdAdministration {
	Map<String, ConfigInfo> resourcesLocked = new HashMap<>();
	
	private int counter = 1000;
	
	public String getConfigId(int entryIdx, List<Resource> entryResources, NavigationPublicPageData currentPage, Resource currentPrimaryResource, Object context,
			Object lastContext, String currentConfigId) {
		String result = ""+counter;
		counter++;
		ConfigInfo newConfig = new ConfigInfo(entryIdx, entryResources);
		if(currentPage == null) {
			//TODO: We assume that we are processing a BackButton at this point, but
			//maybe this condition also applies for other cases
			ConfigInfo currentConfig = getConfigInfo(currentConfigId);
			if(currentConfig != null)
				return currentConfig.lastConfigId;
		}
		//TODO: It has not been tested if these values are still required when going back is
		//implemented by using the ConfigInfo of the page to which we go back.
		newConfig.lastPage = currentPage;
		newConfig.lastPrimaryResource = currentPrimaryResource;
		newConfig.lastContext = lastContext;
		newConfig.lastConfigId = currentConfigId;
		newConfig.context = context;
		resourcesLocked.put(result, newConfig );
		return result;
	}
	
	public ConfigInfo getConfigInfo(String configId) {
		return resourcesLocked.get(configId);
	}
}
