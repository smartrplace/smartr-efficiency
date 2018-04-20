package org.smartrplace.smarteff.admin.protect;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionCapabilityForCreate;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ProviderPublicDataForCreateImpl implements ProviderPublicDataForCreate {
	private final ExtensionCapabilityForCreate inputProvider;
	
	public ProviderPublicDataForCreateImpl(ExtensionCapabilityForCreate inputProvider) {
		this.inputProvider = inputProvider;
	}

	@Override
	public List<EntryType> getEntryTypes() {
		return inputProvider.getEntryTypes();
	}
	
	@Override
	public List<Class<? extends Resource>> createTypes() {
		return inputProvider.createTypes();
	}

	@Override
	public String id() {
		return SPPageUtil.buildId(inputProvider);
	}

	@Override
	public String label(OgemaLocale locale) {
		return inputProvider.label(locale);
	}

	@Override
	public List<Class<? extends Resource>> typesListedInTable() {
		return inputProvider.typesListedInTable();
	}

}
