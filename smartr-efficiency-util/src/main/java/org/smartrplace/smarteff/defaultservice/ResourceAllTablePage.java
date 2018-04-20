package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ResourceAllTablePage extends ResourceTablePage {
	public ResourceAllTablePage() {
		super();
	}
	
	@Override
	protected boolean isInherited() {
		return true;
	}

	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		List<Resource> resultAll = getReqData(req).getSubResources(false);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			if(!(r instanceof ValueResource)) result.add(r);
		}
		return result;
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return ResourceAllTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Generic All-Resource Overview Table";
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "All Resources in "+super.getHeader(req);
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	@Override
	protected PagePriority getPriority() {
		return PagePriority.HIDDEN;
	}
}
