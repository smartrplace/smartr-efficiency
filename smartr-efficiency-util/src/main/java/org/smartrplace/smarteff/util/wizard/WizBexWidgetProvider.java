package org.smartrplace.smarteff.util.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.ObjectResourceGUIHelperExtPublic;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.TypeResult;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.smarteff.util.wizard.AddEditButtonWizardList.BACKTYPE;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.label.Label;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/** Generic EditPageGenericTableWidgetProvider for wizard pages (parent and child)
 * @param T resource type of respective page
 * @param S resource type of child page. The wizard goes through the resources of this type of a resource list below
 * 		the parent.
 * 
 * TODO: Labels and other things are not really generic yet !!
 */
public abstract class WizBexWidgetProvider<T extends Resource, S extends Resource> implements EditPageGenericTableWidgetProvider<T> {
	
	public WizBexWidgetProvider() { //Class<S> type, String entryPageUrl, String editPageURL, String tablePageUrl) {
		//this.typeS = type;
		//this.editPageURL = editPageURL;
		//this.entryPageUrl = editPageURL;
		//this.tablePageUrl = tablePageUrl;
	}
	
	protected void checkResource(T res) {}
	protected abstract Class<S> typeS();
	protected abstract String editPageURL();
	protected abstract String entryPageUrl();
	protected abstract String tablePageUrl();
	protected abstract ApplicationManagerMinimal appManMin();
	protected abstract List<SmartEffTimeSeriesWidgetContext> tsCountersImpl();
	
	public static final String ROOM_FIRST_LABEL_ID = "#roomPageSeries";

	public static final String ROOM_NEXT_LABEL_ID = "#roomNext";
	public static final String ROOM_BACK_LABEL_ID = "#roomBack";
	public static final String ROOM_ENTRY_LABEL_ID = "#roomEntry";
	public static final String ROOM_TABLE_LABEL_ID = "#roomTable";
	public static BACKTYPE getType(String labelId) {
		if(labelId.equals(ROOM_BACK_LABEL_ID)) return BACKTYPE.BACK;
		if(labelId.equals(ROOM_NEXT_LABEL_ID)) return BACKTYPE.FORWARD;
		return BACKTYPE.START;
	}
	
	private ObjectResourceGUIHelperExtPublic<T> mh;
	//private Alert alert;
	//private Map<String, Float> lowerLimits;
	//private Map<String, Float> upperLimits;
	//private Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions;

	//private ApplicationManagerSPExt appManExt;
	private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	private WidgetPage<?> page;
	//private Class<? extends Resource> pageResoureType;

	static OgemaLocale localeDefault = OgemaLocale.ENGLISH;
	public static final Map<OgemaLocale, String> ROOMFIRSTBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> ROOMSTEPBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> ROOMBACKBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> ROOMFINALBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> ROOMENTRYBUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> TABLEOPEN_BUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> ROOMTABLEOPEN_BUTTON_TEXTS = new HashMap<>();
	static {
		ROOMFIRSTBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open first room...");
		ROOMFIRSTBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ersten Raum öffnen...");
		ROOMSTEPBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open next room...");
		ROOMSTEPBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Nächsten Raum öffnen...");
		ROOMBACKBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Previous room...");
		ROOMBACKBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Vorheriger Raum...");
		ROOMFINALBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "No more rooms, finish");
		ROOMFINALBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Keine weiteren Räume, beenden");
		ROOMENTRYBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open room...");
		ROOMENTRYBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Raum öffnen...");

		TABLEOPEN_BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Room Administration Generic...");
		TABLEOPEN_BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Allgemeine Raumverwaltung...");
		ROOMTABLEOPEN_BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Room Administration ...");
		ROOMTABLEOPEN_BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Raumverwaltung...");
	}

	@Override
	public List<CapabilityDeclaration> capabilities() {
		List<CapabilityDeclaration> result = new ArrayList<>();
		result.add(new CapabilityDeclaration(typeS()));
		result.add(new CapabilityDeclaration(ROOM_FIRST_LABEL_ID));
		result.add(new CapabilityDeclaration(ROOM_NEXT_LABEL_ID));
		result.add(new CapabilityDeclaration(ROOM_BACK_LABEL_ID));
		result.add(new CapabilityDeclaration(ROOM_ENTRY_LABEL_ID));
		result.add(new CapabilityDeclaration(ROOM_TABLE_LABEL_ID));
		return result;
	}

	@Override
	public OgemaWidget createValueWidget(String sub, TypeResult type2, Label labelWidgetForValue,
			ObjectResourceGUIHelperExtPublic<T> mhLoc, boolean isEditable,
			boolean isEditableSpecific, String pid) {
		String subId = WidgetHelper.getValidWidgetId(sub);
		if(sub.equals(ROOM_TABLE_LABEL_ID)) {
			ResourceOfTypeTableOpenButton valueWidget = new ResourceOfTypeTableOpenButton(page, "open_"+subId, pid, exPage, null) {
				private static final long serialVersionUID = 1L;

				@Override
				protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData,
						OgemaHttpRequest req) {
					return typeS();
				}
				
				@Override
				protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
						OgemaHttpRequest req) {
					return tablePageUrl();
				}
				
				@Override
				protected Map<OgemaLocale, String> getTextMap(OgemaHttpRequest req) {
					return ROOMTABLEOPEN_BUTTON_TEXTS;
				}
			};
			valueWidget.openResSub(true);
			mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
			return valueWidget;
		}
		if(type2.type == null) {
			AddEditButton valueWidget = new AddEditButtonWizardList<S>(page, "openFirstPage"+subId, pid,
					exPage, null, getType(sub), appManMin()) {
				private static final long serialVersionUID = 1L;

				@Override
				protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
					switch(sub) {
					case ROOM_FIRST_LABEL_ID:
						return ROOMFIRSTBUTTON_TEXTS;
					case ROOM_NEXT_LABEL_ID:
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						if(getResource(appData, req) == null)
							return ROOMFINALBUTTON_TEXTS;
						else
							return ROOMSTEPBUTTON_TEXTS;
					case ROOM_BACK_LABEL_ID:
						return ROOMBACKBUTTON_TEXTS;
					case ROOM_ENTRY_LABEL_ID:
						return ROOMENTRYBUTTON_TEXTS;
					//case ROOM_TABLE_LABEL_ID:
					//	return ROOMTABLEOPEN_BUTTON_TEXTS;
					default: throw new IllegalStateException("Unknown page sub ID:"+sub);
					}
				}
				@Override
				protected Boolean forceEnableState(OgemaHttpRequest req) {
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					if(getResource(appData, req) == null) return true;
					return null;
				}
				@Override
				protected T getEntryResource(OgemaHttpRequest req) {
					T res = mhLoc.getGatewayInfo(req);
					checkResource(res);
					return res;
				}
				@Override
				protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
						OgemaHttpRequest req) {
					return editPageURL();
					//return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
				}
				@Override
				public void setUrl(String url, OgemaHttpRequest req) {
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					if(getResource(appData, req) == null)
						super.setUrl(entryPageUrl(), req);
					else
						super.setUrl(url, req);
				}
				@Override
				protected Class<S> getType() {
					return typeS();
				}
				
				@Override
				protected List<SmartEffTimeSeriesWidgetContext> tsCounters() {
					return tsCountersImpl();
				}
			};
			return valueWidget;
		}
		if(ResourceList.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				ResourceOfTypeTableOpenButton valueWidget = new ResourceOfTypeTableOpenButton(page, "open_"+subId, pid, exPage, null) {
					private static final long serialVersionUID = 1L;

					@Override
					protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData,
							OgemaHttpRequest req) {
						return type2.elementType;
					}
					//@Override
					//protected String getEditPageURL() {
					//	return WizBexWidgetProvider.this.getEditPageURL();
						//return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
					//}
					
					@Override
					protected Map<OgemaLocale, String> getTextMap(OgemaHttpRequest req) {
						return TABLEOPEN_BUTTON_TEXTS;
					}
				};
				valueWidget.openResSub(true);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return new Label(page, "noEdit_"+sub, "Not Allowed");
		} else {
			return null;
		}
	}

	@Override
	public void setGlobalData(ObjectResourceGUIHelperExtPublic<T> mh, Alert alert,
			Map<String, Float> lowerLimits, Map<String, Float> upperLimits,
			Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions, ApplicationManagerSPExt appManExt,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			WidgetPage<?> page,
			Class<? extends Resource> pageResoureType,
			Map<String, Object> widgetContexts) {
		this.mh = mh;
		//this.alert = alert;
		//this.lowerLimits = lowerLimits;
		//this.upperLimits = upperLimits;
		//this.displayOptions = displayOptions;
		//this.appManExt = appManExt;
		this.page = page;
		this.exPage = exPage;
		//this.pageResoureType = pageResoureType;
	}
	
	public static String getLocalString(OgemaLocale locale, Map<OgemaLocale, String> map) {
		String result = map.get(locale);
		if(result != null) return result;
		return map.get(localeDefault);
	}
}
