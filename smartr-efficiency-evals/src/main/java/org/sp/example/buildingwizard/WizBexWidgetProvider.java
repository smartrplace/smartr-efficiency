package org.sp.example.buildingwizard;

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
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.TypeResult;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider;
import org.smartrplace.util.format.WidgetHelper;
import org.sp.example.smarteff.eval.capability.SPEvalDataService;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.label.Label;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingUnit;

/** Special implementation
 */
public class WizBexWidgetProvider<T extends Resource> implements EditPageGenericTableWidgetProvider<T> {
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
	static {
		ROOMFIRSTBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open first room...");
		ROOMFIRSTBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ersten Raum öffnen...");
		ROOMSTEPBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open next room...");
		ROOMSTEPBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Nächsten Raum öffnen...");
		ROOMBACKBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Previous room...");
		ROOMBACKBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Vorheriger Raum...");
		ROOMFINALBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "No more rooms, finish");
		ROOMFINALBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Keine weiteren Räume, beenden");
	}

	@Override
	public List<CapabilityDeclaration> capabilities() {
		List<CapabilityDeclaration> result = new ArrayList<>();
		result.add(new CapabilityDeclaration(BuildingUnit.class));
		result.add(new CapabilityDeclaration(WizBexBuildingEditPage.ROOM_NEXT_LABEL_ID));
		result.add(new CapabilityDeclaration(WizBexRoomEditPage.ROOM_NEXT_LABEL_ID));
		result.add(new CapabilityDeclaration(WizBexRoomEditPage.ROOM_BACK_LABEL_ID));
		return result;
	}

	@Override
	public OgemaWidget createValueWidget(String sub, TypeResult type2, Label labelWidgetForValue,
			ObjectResourceGUIHelperExtPublic<T> mhLoc, boolean isEditable,
			boolean isEditableSpecific, String pid) {
		String subId = WidgetHelper.getValidWidgetId(sub);
		if(type2.type == null) {
			AddEditButton valueWidget = new AddEditButtonWizardList<T>(page, "openFirstPage"+subId, pid,
					exPage, null, (sub.equals(WizBexRoomEditPage.ROOM_BACK_LABEL_ID))) {
				private static final long serialVersionUID = 1L;

				@Override
				protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
					switch(sub) {
					case WizBexBuildingEditPage.ROOM_NEXT_LABEL_ID:
						return ROOMFIRSTBUTTON_TEXTS;
					case WizBexRoomEditPage.ROOM_NEXT_LABEL_ID:
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						if(getResource(appData, req) == null)
							return ROOMFINALBUTTON_TEXTS;
						else
							return ROOMSTEPBUTTON_TEXTS;
					case WizBexRoomEditPage.ROOM_BACK_LABEL_ID:
						return ROOMBACKBUTTON_TEXTS;
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
					return mhLoc.getGatewayInfo(req);
				}
				@Override
				protected String getDestinationURL(ExtensionResourceAccessInitData appData, Resource object,
						OgemaHttpRequest req) {
					return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
				}
				@Override
				public void setUrl(String url, OgemaHttpRequest req) {
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					if(getResource(appData, req) == null)
						super.setUrl("org_sp_example_buildingwizard_WizBexEntryPage.html", req);
					else
						super.setUrl(url, req);
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
					@Override
					protected String getEditPageURL() {
						return SPPageUtil.getProviderURL(SPEvalDataService.WIZBEX_ROOM.provider);
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
			Class<? extends Resource> pageResoureType) {
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
