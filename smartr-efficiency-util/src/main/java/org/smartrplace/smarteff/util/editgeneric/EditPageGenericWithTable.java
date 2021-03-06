package org.smartrplace.smarteff.util.editgeneric;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.smarteff.defaultservice.ResourceTablePage;

import de.iwes.widgets.api.widgets.WidgetPage;
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

	//override if required
	protected GenericResourceByTypeTablePageBase createTablePage() {
		return new GenericResourceByTypeTablePage<T>(this, this.getClass().getName()+"_TablePage");
	}
	/** Return true if the standard {@link ResourceTablePage#addWidgetsAboveTable} shall not be
	 * executed to avoid standard navigation buttons
	 * @param resourceType
	 * @return
	 */
	public boolean addWidgetsAboveTable(Class<? extends Resource> resourceType,
			WidgetPage<?> page,
			GenericResourceByTypeTablePage<T> genericResourceByTypeTablePage) {
		return false;
	}
	/** Set to false if table shall not add delete buttons to rows*/
	public boolean offerDeleteInTable() {
		return true;
	}

	Map<String, Map<OgemaLocale, String>> tableHeaders = new LinkedHashMap<>();
	
	private GenericResourceByTypeTablePageBase genericTablePageWrapper;
	private final boolean isWithTable;
	
	public EditPageGenericWithTable() {
		this(new Boolean(true));
	}
	public EditPageGenericWithTable(Boolean forceSubResName) {
		this(true, forceSubResName);
	}
	public EditPageGenericWithTable(boolean isWithTable) {
		this(isWithTable, new Boolean(true));
	}
	public EditPageGenericWithTable(boolean isWithTable, Boolean forceSubResName) {
		super(forceSubResName);
		this.isWithTable = isWithTable;
	}
	public EditPageGenericWithTable(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders,
			boolean isWithTable) {
		this(additionalWidgetProviders, isWithTable, new Boolean(true));
	}
	public EditPageGenericWithTable(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders,
			boolean isWithTable, Boolean forceSubResName) {
		super(additionalWidgetProviders, forceSubResName);
		this.isWithTable =isWithTable;
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
	
	public GenericResourceByTypeTablePageBase getTablePage() {
		if(!isWithTable) return null;
		if(genericTablePageWrapper != null) return genericTablePageWrapper;
		genericTablePageWrapper = createTablePage();
		return genericTablePageWrapper;
	}
	
	@Override
	protected void checkSetData() {
		boolean isIDinTable = tableHeaders.containsKey("name");
		if(!isIDinTable) {
			for(Map<OgemaLocale, String> innerMap: tableHeaders.values()) {
				String enval = innerMap.get(EN);
				if(enval != null && enval.toLowerCase().equals("name")) {
					isIDinTable = true;
					break;
				}
			}
		}
		if(!isIDinTable) {
			Map<String, Map<OgemaLocale, String>> oldMap = tableHeaders;
			Map<OgemaLocale, String> innerMap = new HashMap<>();
			tableHeaders = new LinkedHashMap<>();
			tableHeaders.put("name", innerMap);
			tableHeaders.putAll(oldMap);
			innerMap.put(EN, "Name");

			//setTableHeader("name", EN, "Name");
		}
	}
}
