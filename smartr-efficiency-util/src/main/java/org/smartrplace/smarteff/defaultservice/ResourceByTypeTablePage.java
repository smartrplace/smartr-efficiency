package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserData;

public class ResourceByTypeTablePage extends ResourceTablePage {
	public ResourceByTypeTablePage() {
		super();
	}
	
	protected Class<? extends Resource> typeSelected(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(appData.getConfigInfo().context == null) throw new IllegalStateException("Context required for resource type!");
		if(!(appData.getConfigInfo().context instanceof String)) throw new IllegalStateException("Type must be transmitted as String!");
		String param = (String)appData.getConfigInfo().context;
		for(ExtensionResourceTypeDeclaration<?> decl: appManExt.getAllTypeDeclararions()) {
			if(decl.dataType().getName().equals(param)) return decl.dataType();
		}
		return null;
	}
	
	@Override
	protected boolean isInherited() {
		return true;
	}

	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		List<? extends Resource> resultAll = ((SmartEffUserData)appData.userData()).getSubResources(typeSelected(req), true);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			result.add(r);
		}
		return result;
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return ResourceByTypeTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Generic All-Resource Overview Table";
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Resources for "+typeSelected(req);
	}
	
	@Override
	protected PagePriority getPriority() {
		return PagePriority.HIDDEN;
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return null;
	}
	
	@Override
	protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
	}
}
