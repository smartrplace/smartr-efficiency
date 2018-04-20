package org.smartrplace.smarteff.util.editgeneric;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class EditPageGenericWithTable<T extends Resource> extends EditPageGeneric<T> {
	Map<String, Map<OgemaLocale, String>> tableHeaders = new LinkedHashMap<>();
	
	private GenericResourceByTypeTablePageBase<T> genericTablePageWrapper;
	private final boolean isWithTable;
	
	public EditPageGenericWithTable() {
		this(true);
	}
	public EditPageGenericWithTable(boolean isWithTable) {
		super();
		this.isWithTable = isWithTable;
	}
	
	protected void setTableHeader(String resourceName, OgemaLocale locale, String text) {
		Map<OgemaLocale, String> innerMap = tableHeaders.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			tableHeaders.put(resourceName, innerMap);
		}
		innerMap.put(locale, text);
	}
	protected void setTableHeader(Resource res, OgemaLocale locale, String text) {
		setTableHeader(getSubPath(res), locale, text);
	}
	protected void setTableHeader(String resourceName, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setTableHeader(resourceName, locale, text);
		setTableHeader(resourceName, locale2, text2);
	}
	protected void setTableHeader(Resource res, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setTableHeader(getSubPath(res), locale, text, locale, text);		
		setTableHeader(getSubPath(res), locale, text, locale2, text2);		
	}
	
	@Override
	protected void addWidgets() {
		super.addWidgets();
		if(!isWithTable) return;
		if(getTablePage().providerInitDone && (!getTablePage().triggerForTablePageDone)) {
			getTablePage().triggerForTablePageDone = true;
			getTablePage().triggerPageBuild();
		}
	}
	
	public GenericResourceByTypeTablePageBase<T> getTablePage() {
		if(!isWithTable) return null;
		if(genericTablePageWrapper != null) return genericTablePageWrapper;
		genericTablePageWrapper = new GenericResourceByTypeTablePage<T>(this);
		return genericTablePageWrapper;
	}
	
	// public methods for GenericResourceByTypeTablePage
	/*public ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> getExPage() {
		return exPage;
	}
	
	public ExtensionResourceAccessInitData getAppData(OgemaHttpRequest req) {
		return exPage.getAccessData(req);
	}*/
	
	public Class<? extends Resource> getPrimaryEntryTypeClass() {
		return primaryEntryTypeClass();
	}
}
