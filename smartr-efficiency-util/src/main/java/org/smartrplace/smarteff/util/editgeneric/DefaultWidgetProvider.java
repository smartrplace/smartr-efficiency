package org.smartrplace.smarteff.util.editgeneric;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.SpartEffModelModifiers.DataType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.TSManagementPage;
import org.smartrplace.smarteff.defaultservice.TSManagementPage.ContextType;
import org.smartrplace.smarteff.defaultservice.TSManagementPage.SubmitTSValueButton;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.ObjectResourceGUIHelperExtPublic;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.AddEditButtonForCreate;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.TypeResult;
import org.smartrplace.util.format.ValueConverter;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.multiselect.extended.MultiSelectStringArrayFreeText;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/** Default and example implementation of {@link EditPageGenericTableWidgetProvider}<br>
 * Supported special IDs not related to a certain sub-resource:
 * <li> #humanreadable : show {@link ResourceUtils#getHumanReadableName(Resource)}
 * <li> #humanreadableshort : show {@link ResourceUtils#getHumanReadableShortName(Resource)}
 * <li> #location : get resource location
 */
public class DefaultWidgetProvider<T extends Resource> implements EditPageGenericTableWidgetProvider<T> {
	private ObjectResourceGUIHelperExtPublic<T> mh;
	private Alert alert;
	private Map<String, Float> lowerLimits;
	private Map<String, Float> upperLimits;
	private Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions;

	private ApplicationManagerSPExt appManExt;
	private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	private WidgetPage<?> page;
	private Class<? extends Resource> pageResoureType;

	static OgemaLocale localeDefault = OgemaLocale.ENGLISH;

	@Override
	public List<CapabilityDeclaration> capabilities() {
		//We do not care about capabilities here
		return null;
	}

	@Override
	public OgemaWidget createValueWidget(String sub, TypeResult type2, Label labelWidgetForValue,
			ObjectResourceGUIHelperExtPublic<T> mhLoc, boolean isEditable,
			boolean isEditableSpecific, String pid) {
		String subId = WidgetHelper.getValidWidgetId(sub);
		if(type2.type == null) {
			Label specialLabel = new Label(page, "specialLabel"+subId) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					String text;
					T entryResource = mhLoc.getGatewayInfo(req);
					switch(sub) {
					case "#humanreadable":
						text = ResourceUtils.getHumanReadableName(entryResource);
						break;
					case "#humanreadableshort":
						text = ResourceUtils.getHumanReadableShortName(entryResource);
						break;
					case "#location":
						text = entryResource.getLocation();
						break;
					default:
						text = "Unknown:"+sub;
						break;
					}
					setText(text, req);
				}
			};
			return specialLabel;
		}
		if(StringResource.class.isAssignableFrom(type2.type)) {
			//if(isEditableSpecific)
			if(isEditable)	return mhLoc.stringEdit(sub, alert);
			else return mhLoc.stringLabel(sub);
		} else if(FloatResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?low:0;
				float upv = (up!=null)?up:999999f;
				TextField valueWidget = mhLoc.floatEdit((String)sub, alert, lowv, upv,
						sub+" limits:"+lowv+" to "+upv);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.floatLabel(sub, "%.2f");
		} else if(BooleanResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				BooleanResourceCheckbox valueWidget = mhLoc.booleanEdit((String)sub);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.resourceLabel(sub, 20);
		} else if(StringResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				TextField valueWidget = mhLoc.stringEdit((String)sub, alert);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.stringLabel(sub);
		} else if(IntegerResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				Map<OgemaLocale, Map<String, String>> innerMap = displayOptions.get(sub);
				if(innerMap != null) {
					ValueResourceDropdown<IntegerResource> valueWidget = new ValueResourceDropdown<IntegerResource>(page, "drop"+subId) {
						private static final long serialVersionUID = 1L;
						public void onGET(OgemaHttpRequest req) {
							IntegerResource source = ResourceHelper.getSubResource(mhLoc.getGatewayInfo(req), sub, IntegerResource.class);
							selectItem(source, req);
							Map<String, String> valuesToSet = innerMap.get(req.getLocale());
							if(valuesToSet == null) valuesToSet = innerMap.get(localeDefault);
							setDisplayedValues(new ArrayList<>(valuesToSet.values()), req);
						}
						@Override
						public void onPrePOST(String data, OgemaHttpRequest req) {
							IntegerResource source = ResourceHelper.getSubResource(mhLoc.getGatewayInfo(req), sub, IntegerResource.class);
							if(!source.exists()) {
								source.create();
								source.activate(true);
							}
						}
						@Override
						public String getSelection(IntegerResource resource, Locale locale, List<String> displayedValues) {
							OgemaLocale loc = OgemaLocale.getLocale(locale.getLanguage());
							Map<String, String> valuesToSet = innerMap.get(loc);
							if(valuesToSet == null) valuesToSet = innerMap.get(localeDefault);
							if(valuesToSet == null) return super.getSelection(resource, locale, displayedValues);
							String value = ValueResourceUtils.getValue(resource);
							String display = valuesToSet.get(value);
							if(display == null) return displayedValues.get(0);
							return display;
						}
						@Override
						protected void setResourceValue(IntegerResource resource, String value, List<String> displayedValues) {
							//TODO: Make this more efficient with a new widget
							for(Map<String, String> valuesToSet : innerMap.values()) {
								for(Entry<String, String> e: valuesToSet.entrySet()) {
									if(e.getValue().equals(value)) {
										ValueResourceUtils.setValue(resource, e.getKey());
										return;
									}
								}
							}
							super.setResourceValue(resource, value, displayedValues);
						}
					};
					//mh.dropdown(sub, valuesToSet, IntegerResource.class);
					mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
					return valueWidget;
				} else {
					Float low = lowerLimits.get(sub);
					Float up = upperLimits.get(sub);
					int lowv = (low!=null)?(int)(float)low:0;
					int upv = (up!=null)?(int)(float)up:999999;
					ValueConverter checker = new ValueConverter(sub, alert, (float)lowv, (float)upv) {
						@Override
						protected String getFieldName(OgemaHttpRequest req) {
							if(labelWidgetForValue != null)
								return labelWidgetForValue.getText(req);
							else return sub;
						}
					};
					TextField valueWidget = mhLoc.integerEditExt((String)sub, checker);
					mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
					return valueWidget;
				}
			} else return mhLoc.intLabel(sub, 0);
		} else if(TimeResource.class.isAssignableFrom(type2.type)) {
			if(sub.contains("Duration")||sub.contains("Interval")) {
				if(isEditable)	{
					TextField valueWidget = mhLoc.timeEdit((String)sub, null, 0l, Long.MAX_VALUE, "Interval ragen invalid!", -1);
					mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
					return valueWidget;
				} else
					return mhLoc.timeLabel(sub, 1);
			}

			if(isEditable)	{
				final String format;
				if(sub.contains("Day")) format = "YYYY-MM-DD";
				else format = "YYYY-MM-DD HH:mm:ss";
				DatepickerTimeResource valueWidget = mhLoc.datepicker((String)sub, format, (String)null, (String)null);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.timeLabel(sub, 0);
		} else if(SmartEffTimeSeries.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				//if(sub.toLowerCase().contains("csv")) {
					//TODO: Move this to special WidgetProvider, should usually not be used
				final CSVUploadWidgets csvData;
				final OgemaWidget newValue;
				final Button newValueButton;
				
				csvData = new CSVUploadWidgets(exPage, page, alert, subId+pid,
						"Upload Profile as CSV", null) {
					
					@Override
					protected SmartEffTimeSeries getTSResource(OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						SmartEffTimeSeries tsResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, SmartEffTimeSeries.class);
						return tsResource;
					}
				};
				csvData.csvButton.setDefaultVisibility(false);
				csvData.uploader.getFileUpload().setDefaultVisibility(false);														
				//RedirectButton openEvalButton = new LogicProvTableOpenButton(page, "openTSManButton"+sub, pid,
				//		exPage, null);
				
				Map<OgemaLocale, Map<String, String>> innerMap = displayOptions.get(sub);
				if(innerMap != null) {
					newValue = new TSManagementPage.AddValueDropdown(page, "newValueSP"+subId+pid, innerMap);
				} else {				
					 newValue = new TextField(page, "newValueSP"+subId+pid);
				}		
				newValue.setDefaultVisibility(false);

				newValueButton = new SubmitTSValueButton(page, "newValueButton"+subId+pid,
						newValue, null, null, alert, appManExt) {
					private static final long serialVersionUID = 1L;

					@Override
					protected boolean submitForNow(OgemaHttpRequest req) {
						return true;
					}
					
					@Override
					protected SmartEffTimeSeries getResource(OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						SmartEffTimeSeries tsResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, SmartEffTimeSeries.class);
						return tsResource;
					}
					
					@Override
					protected Class<? extends Resource> getType(OgemaHttpRequest req) {
						//T entryResource = mhLoc.getGatewayInfo(req);
						try {
							Class<? extends Resource> ptype = pageResoureType;
							Method method = ptype.getMethod(sub); //entryResource.getClass().getMethod(sub);
							DataType an = method.getAnnotation(DataType.class);
							if(an == null) return FloatResource.class;
							return an.resourcetype();
						} catch (NoSuchMethodException | SecurityException e) {
							return FloatResource.class;
						}
					}
					
					@Override
					protected boolean disableForSure(OgemaHttpRequest req) {
						return newValue.isDisabled(req);
					}
					
					/*@Override
					public void onGET(OgemaHttpRequest req) {
						SmartEffTimeSeries tsResource = getResource(req);
						if(tsResource == null || (!tsResource.schedule().isActive())) {
							newValue.setWidgetVisibility(false, req);
							return;
						}
						newValue.setWidgetVisibility(true, req);
						super.onGET(req);
					}*/
				};
				//newValueButton.setDefaultVisibility(false);
				
				RedirectButton openTSManButton = new AddEditButton(page, "openEvalButton"+subId, pid,
						exPage, null) {
					private static final long serialVersionUID = 1L;
					@Override
					public void updateDependentWidgets(OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						SmartEffTimeSeries tsResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, SmartEffTimeSeries.class);
						//SmartEffTimeSeries tsResource = ResourceHelper.getSubResource(entryResource, sub, SmartEffTimeSeries.class);
						if(tsResource == null) {
							newValue.setWidgetVisibility(false, req);
							newValueButton.setWidgetVisibility(false, req);
							csvData.csvButton.setWidgetVisibility(false, req);
							csvData.uploader.getFileUpload().setWidgetVisibility(false, req);														
						} else if(tsResource.schedule().isActive()) {
							newValue.setWidgetVisibility(true, req);
							newValueButton.setWidgetVisibility(true, req);
							csvData.csvButton.setWidgetVisibility(false, req);
							csvData.uploader.getFileUpload().setWidgetVisibility(false, req);							
						} else {
							newValue.setWidgetVisibility(false, req);
							newValueButton.setWidgetVisibility(false, req);
							if(tsResource.fileType().isActive()) {
								csvData.csvButton.setWidgetVisibility(true, req);
								csvData.uploader.getFileUpload().setWidgetVisibility(true, req);
							} else {
								csvData.csvButton.setWidgetVisibility(false, req);
								csvData.uploader.getFileUpload().setWidgetVisibility(false, req);							
							}
						}
					}
					@Override
					protected Resource getResource(ExtensionResourceAccessInitData appData,
							OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						SmartEffTimeSeries tsResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, SmartEffTimeSeries.class);
						//SmartEffTimeSeries tsResource = ResourceHelper.getSubResource(entryResource, sub, SmartEffTimeSeries.class);
						return tsResource;
					}
					@Override
					protected Object getContext(ExtensionResourceAccessInitData appData, Resource object,
							OgemaHttpRequest req) {
						try {
							ContextType ctt = new ContextType();
							Class<? extends Resource> ptype = pageResoureType;
							final Method method;
							if(sub.contains("/")) {
								T entryResource = mhLoc.getGatewayInfo(req);
								SmartEffTimeSeries tsResource = CapabilityHelper.getOrcreateResource(entryResource,
										sub, appData.systemAccess(), appManExt, SmartEffTimeSeries.class);
								ptype = tsResource.getParent().getResourceType();
								String lastSub = sub.substring(sub.lastIndexOf('/')+1);
								method = ptype.getMethod(lastSub);
								//String newSub = sub.substring(0, sub.lastIndexOf('/'));
								//newParent = CapabilityHelper.getOrcreateResource(parent,
								//		newSub, appData.systemAccess(), appManExt, resultType);
							}
							else method = ptype.getMethod(sub); //entryResource.getClass().getMethod(sub);
							DataType an = method.getAnnotation(DataType.class);
							if(an == null) ctt.type = null; //super.getContext(appData, object, req);
							else ctt.type = an.resourcetype();
							ctt.innerMap = innerMap;
							return ctt;
						} catch (NoSuchMethodException | SecurityException e) {
							return super.getContext(appData, object, req);
						}
						//return super.getContext(appData, object, req);
					}
					@Override
					protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
						return TSManagementPage.SUPERBUTTON_TEXTS;
					}
				};
				
				mhLoc.triggerOnPost(openTSManButton, csvData.csvButton);
				mhLoc.triggerOnPost(openTSManButton, csvData.uploader.getFileUpload());
				mhLoc.triggerOnPost(openTSManButton, newValue);
				mhLoc.triggerOnPost(newValue, newValueButton);
				mhLoc.triggerOnPost(newValueButton, alert);
				
				csvData.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);
				newValueButton.setDefaultPadding("1em", false, true, false, true);
				Flexbox valueWidget = TSManagementPage.getHorizontalFlexBox(page, "flexbox"+subId+pid,
						csvData.csvButton, newValue, csvData.uploader.getFileUpload(), newValueButton, openTSManButton);
				return valueWidget;
			} else return new Label(page, "noEdit_"+sub, "No Upload Allowed");
		} else if(ResourceList.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				ResourceOfTypeTableOpenButton valueWidget = new ResourceOfTypeTableOpenButton(page, "open_"+sub, pid, exPage, null) {
					private static final long serialVersionUID = 1L;

					@Override
					protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData,
							OgemaHttpRequest req) {
						return type2.elementType;
					}
				};
				valueWidget.openResSub(true);
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return new Label(page, "noEdit_"+sub, "Not Allowed");
		} else if(StringArrayResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				MultiSelectStringArrayFreeText multi = new MultiSelectStringArrayFreeText(page, "multisel"+sub) {
					@Override
					protected StringArrayResource getStringArrayResource(OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						StringArrayResource tsResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, StringArrayResource.class);
						return tsResource;
					}
				};
				Flexbox valueWidget = EditPageBase.getHorizontalFlexBox(page, "flexbox"+sub+pid,
						multi.multiSelect, multi.newValue, multi.submit);
				return valueWidget;
			} else return mhLoc.stringLabel(sub);
		} else if(SmartEffResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				AddEditButtonForCreate valueWidget = new AddEditButtonForCreate(page, "open_"+sub, pid, type2.type, exPage, null) {
					private static final long serialVersionUID = 1L;
					
					@Override
					protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
						T entryResource = mhLoc.getGatewayInfo(req);
						Resource destResource = CapabilityHelper.getOrcreateResource(entryResource,
								sub, appData.systemAccess(), appManExt, type2.type);
						//Resource destResource = ResourceHelper.getSubResource(entryResource, sub, type2.type);
						return destResource;
					}
				};
				mh.triggerOnPost(valueWidget, valueWidget); //valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return new Label(page, "noEdit_"+sub, "Not Allowed");
		} else {
			return new Label(page, "noEdit_"+sub, "No Widget to edit "+sub+" of type "+type2.type.getSimpleName());
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
		this.alert = alert;
		this.lowerLimits = lowerLimits;
		this.upperLimits = upperLimits;
		this.displayOptions = displayOptions;
		this.appManExt = appManExt;
		this.page = page;
		this.exPage = exPage;
		this.pageResoureType = pageResoureType;
	}
	
	public static String getLocalString(OgemaLocale locale, Map<OgemaLocale, String> map) {
		String result = map.get(locale);
		if(result != null) return result;
		return map.get(localeDefault);
	}
}
