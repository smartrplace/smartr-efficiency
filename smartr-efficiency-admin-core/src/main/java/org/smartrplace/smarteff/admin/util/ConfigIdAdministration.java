package org.smartrplace.smarteff.admin.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData.ConfigInfo;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

/** TODO: This is not thread-safe yet
 * TODO: This has no clean-up mechanism yet
 * TODO: Generate configId randomly individually*/
public class ConfigIdAdministration {
	Map<String, ConfigInfo> resourcesLocked = new HashMap<>();
	
	private int counter = 1000;
	
	public String getConfigId(int entryIdx, List<Resource> entryResources, NavigationPublicPageData currentPage, Resource currentPrimaryResource, Object context) {
		String result = ""+counter;
		counter++;
		ConfigInfo newConfig = new ConfigInfo(entryIdx, entryResources);
		newConfig.lastPage = currentPage;
		newConfig.lastPrimaryResource = currentPrimaryResource;
		newConfig.context = context;
		resourcesLocked.put(result, newConfig );
		return result;
	}
	
	public ConfigInfo getConfigInfo(String configId) {
		return resourcesLocked.get(configId);
	}
}
