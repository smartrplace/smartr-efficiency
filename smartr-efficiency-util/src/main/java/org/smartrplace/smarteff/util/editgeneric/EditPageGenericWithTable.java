package org.smartrplace.smarteff.util.editgeneric;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Use this template to create a table overview page together with an edit page for a
 * resource type. To declare special table headers use the method {@link #setTableHeader(Resource, OgemaLocale, String)}
 * and its derivates. To register the page you have to add the table page declaration obtained via
 * {@link #getTablePage()} in the getCapabilities method of the {@link SmartEffExtensionService}.
 */
public abstract class EditPageGenericWithTable<T extends Resource> extends EditPageGeneric<T> {
	public static final Map<OgemaLocale, String> SUPEREDITBUTTON_TEXTS = new HashMap<>();
	static {
		SUPEREDITBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Step Up");
		SUPEREDITBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ebene hoch");
	}

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
	
	//Overwrite
	public Map<OgemaLocale, String> getSuperEditButtonTexts() {
		return SUPEREDITBUTTON_TEXTS;
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
		genericTablePageWrapper = new GenericResourceByTypeTablePage<T>(this, this.getClass().getName()+"_TablePage");
		return genericTablePageWrapper;
	}
	
	/** Usually this method is not relevant and needs not to be overwritten. The method
	 * primaryEntryTypeClass is not public, which is required for the table page creation.*/
	public Class<? extends Resource> primaryEntryTypeClassPublic() {
		return primaryEntryTypeClass();
	}
}
