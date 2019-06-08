package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserData;

public class TopConfigTablePage extends ResourceTablePage {
	@Override
	protected Resource getFixedParentResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return appData.userData();
	}
	
	public TopConfigTablePage() {
		super();
	}

	@Override
	protected boolean isInherited() {
		return true;
	}

	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		List<Resource> resultAll = ((SmartEffUserData)appData.userData()).getSubResources(false);
		//List<Resource> resultAll = getReqData(req).getSubResources(false);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			if(!((r instanceof ValueResource) || (r instanceof ResourceList))) result.add(r);
		}
		
		//Also add virtual resources for global resources
		List<Resource> globals = appManExt.globalData().getSubResources(false);
		for(Resource glob: globals) {
			Resource r = CapabilityHelper.getForUserVirtual(glob, appData.userData());
			if(!((r instanceof ValueResource) || (r instanceof ResourceList))) result.add(r);
		}
		
		return result;
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return TopConfigTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Top Level Config Overview Table";
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Top Level config resources for "+super.getUserName(req);
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return null;
	}
	
	@Override
	protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
	}
}
