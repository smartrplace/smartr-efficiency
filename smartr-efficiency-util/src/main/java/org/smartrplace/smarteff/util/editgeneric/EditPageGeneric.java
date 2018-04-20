package org.smartrplace.smarteff.util.editgeneric;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.button.ResourceOfTypeTableOpenButton;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider.ColumnType;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider.Visibility;
import org.smartrplace.util.format.ValueConverter;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;

public abstract class EditPageGeneric<T extends Resource> extends EditPageBase<T> {
	public static OgemaLocale EN = OgemaLocale.ENGLISH;
	public static OgemaLocale DE = OgemaLocale.GERMAN;
	public static OgemaLocale FR = OgemaLocale.FRENCH;
	public static OgemaLocale CN = OgemaLocale.CHINESE;

	protected static final String HEADER_LABEL_ID = "#L";
	protected static final String HEADER_LINK_ID = "#H";

	public static final Map<OgemaLocale, String> LINK_BUTTON_TEXTS = new HashMap<>();
	static {
		LINK_BUTTON_TEXTS.put(EN, "Info in Wiki");
		LINK_BUTTON_TEXTS.put(DE, "Info im Wiki");
		LINK_BUTTON_TEXTS.put(FR, "Info en Wiki");
	}

	private T sampleResource;
	private int sampleResourceLength;
	
	/** In this method the page data shall be provided with methods like getLabel
	 * @param sr virtual resource that shall be used to provide paths
	 */
	public abstract void setData(T sr);
	
	Map<String, Map<OgemaLocale, String>> labels = new LinkedHashMap<>();
	Map<String, Map<OgemaLocale, String>> links = new HashMap<>();
	Map<String, Float> lowerLimits = new HashMap<>();
	Map<String, Float> upperLimits = new HashMap<>();
	Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions = new HashMap<>();
	
	Map<String, EditLineProvider> providers = new HashMap<>();
	//Widgets that are governed by an EditLineProvider
	Map<String, OgemaWidget> labelWidgets = new HashMap<>();
	//Subs that shall trigger alert2 that triggers all widgets
	List<String> triggeringSubs = new ArrayList<>();
	
	protected boolean defaultIsEditable = true;
	Map<String, Boolean> isEditable = new HashMap<>();
	
	OgemaLocale localeDefault = OgemaLocale.ENGLISH;
	
	@SuppressWarnings("unchecked")
	private void fillMap(Map<String, Class<? extends Resource>> typeMap, Class<? extends Resource> resType) {
		//typeMap.put("name", StringResource.class);
		for(Method m: resType.getMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(Resource.class.isAssignableFrom(rawClass) && (m.getParameterCount()==0)) {
				typeMap.put(m.getName(), (Class<? extends Resource>) rawClass);				
			}
		}
	}
	Map<String, Class<? extends Resource>> typesGlob = null;
	Map<String, Class<? extends Resource>> getTypes() {
		if(typesGlob == null) {
			typesGlob = new HashMap<>();
			fillMap(typesGlob, primaryEntryTypeClass());
		}
		return typesGlob;
	}
	//ResourceType -> (Element Name -> Type)
	Map<String, Map<String, Class<? extends Resource>>> subTypesGlob = new HashMap<>();
	Map<String, Class<? extends Resource>> getSubTypes(Class<? extends Resource> parentType) {
		String typeStr = parentType.getName();
		Map<String, Class<? extends Resource>> subTypeMap = subTypesGlob.get(typeStr);
		if(subTypeMap == null) {
			subTypeMap = new HashMap<>();
			subTypesGlob.put(typeStr, subTypeMap);
			fillMap(subTypeMap, parentType);
		}
		return subTypeMap;
	}
	class TypeResult {
		public Class<? extends Resource> type;
		public TypeResult(Class<? extends Resource> type) {
			this.type = type;
		}
		public Class<? extends Resource> elementType = null;
	}
	protected TypeResult getType(String subPath) {
		String[] els = subPath.split("/");
		Class<? extends Resource> cr = primaryEntryTypeClass();
		for(int i=0; i<=els.length; i++) {
			if(cr == null) return null;
			if(i == els.length) {
				if(ResourceList.class.isAssignableFrom(cr)) {
					TypeResult result = new TypeResult(cr);
					result.elementType = CapabilityHelper.getTypeFromName(els[i-1], appManExt).dataType();
					return result;
				}
				return new TypeResult(cr);
			}
			//we cannot go over ValueResources and ResourceLists here
			if(ValueResource.class.isAssignableFrom(cr)) throw new IllegalStateException("Path "+subPath+" includes a ValueResouce in middle!");
			if(ResourceList.class.isAssignableFrom(cr)) throw new IllegalStateException("Path "+subPath+" includes a ResourceList in middle!");
			cr = getSubTypes(cr).get(els[i]);
		}
		//if(els.length == 1) return new TypeResult(cr);
		throw new IllegalStateException("we should never get here");		
	}
	
	protected void setLabel(String resourceName, OgemaLocale locale, String text) {
		Map<OgemaLocale, String> innerMap = labels.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			labels.put(resourceName, innerMap);
		}
		innerMap.put(locale, text);
	}
	protected void setLabel(Resource res, OgemaLocale locale, String text) {
		setLabel(getSubPath(res), locale, text);
	}
	protected void setHeaderLabel(OgemaLocale locale, String text) {
		setLabel(HEADER_LABEL_ID, locale, text);
	}
	protected void setLabel(String resourceName, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setLabel(resourceName, locale, text);
		setLabel(resourceName, locale2, text2);
	}
	protected void setHeaderLabel(OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setLabel(HEADER_LABEL_ID, locale, text);
		setLabel(HEADER_LABEL_ID, locale2, text2);
	}
	protected void setLabel(Resource res, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setLabel(getSubPath(res), locale, text, locale2, text2);		
	}
	protected void setLabel(String resourceName, OgemaLocale locale, String text,
			OgemaLocale locale2, String text2, float min, float max) {
		setLabel(resourceName, locale, text);
		setLabel(resourceName, locale2, text2);
		setLimits(resourceName, min, max);
	}
	protected void setLabel(Resource res, OgemaLocale locale, String text,
			OgemaLocale locale2, String text2, float min, float max) {
		setLabel(getSubPath(res), locale, text, locale2, text2, min, max);
	}
	protected void setLink(String resourceName, OgemaLocale locale, String text) {
		Map<OgemaLocale, String> innerMap = links.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			links.put(resourceName, innerMap);
		}
		innerMap.put(locale, text);		
	}
	protected void setLink(Resource res, OgemaLocale locale, String text) {
		setLink(getSubPath(res), locale, text);
	}
	protected void setHeaderLink(OgemaLocale locale, String text) {
		setLink(HEADER_LINK_ID, locale, text);
	}
	protected void setLimits(String resourceName, float min, float max) {
		lowerLimits.put(resourceName, min);		
		upperLimits.put(resourceName, max);		
	}
	protected void setLimits(Resource res, float min, float max) {
		setLimits(getSubPath(res), min, max);
	}
	protected void setLineProvider(String resourceName, EditLineProvider lineProvider) {
		providers.put(resourceName, lineProvider);
	}
	protected void setLineProvider(Resource res, EditLineProvider lineProvider) {
		setLineProvider(getSubPath(res), lineProvider);
	}
	protected void setDisplayOptions(String resourceName, OgemaLocale locale, Map<String, String> options) {
		Map<OgemaLocale, Map<String, String>> innerMap = displayOptions.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			displayOptions.put(resourceName, innerMap);
		}
		innerMap.put(locale, options);		
	}
	protected void setDisplayOptions(Resource res, OgemaLocale locale, Map<String, String> options) {
		setDisplayOptions(getSubPath(res), locale, options);
	}
	
	protected void setTriggering(String resourceName) {
		triggeringSubs.add(resourceName);
	}
	protected void setTriggering(Resource res) {
		setTriggering(getSubPath(res));
	}
	
	public void setDefaultLocale(OgemaLocale locale) {
		localeDefault = locale;
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return primaryEntryTypeClass().getSimpleName()+" Edit Page";
	}
	
	public boolean checkResource(T res) {
		if(!checkResourceBase(res, false)) return false;
		String newName = CapabilityHelper.getnewDecoratorName(primaryEntryTypeClass().getSimpleName(), res.getParent());
		Resource name = res.getSubResource("name");
		if ((name != null) && (name instanceof StringResource)) {
			ValueResourceHelper.setIfNew((StringResource)name, newName);
		}
		//Map<String, Class<? extends Resource>> types = getTypes();
		for(String sub: labels.keySet()) {
			TypeResult type = getType(sub);
			if(type == null) continue;
			if(FloatResource.class.isAssignableFrom(type.type)) {
				Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?up:0;
				float upv = (up!=null)?up:999999f;
				FloatResource valRes = res.getSubResource(sub);
				float val = valRes.getValue();
				if(Float.isNaN(val) || (val < lowv)||(val > upv)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	protected void getEditTableLines(EditTableBuilder etb) {
		//Map<String, Class<? extends Resource>> types = getTypes();
		for(String sub: labels.keySet()) {
			if(sub.equals(HEADER_LABEL_ID) || sub.equals(HEADER_LINK_ID)) continue;
			OgemaWidget valueWidget = null;
			OgemaWidget label = null;
			OgemaWidget linkButton = null;
			TypeResult type = getType(sub);
			if(type == null)
				continue;
			
			EditLineProvider widgetProvider = providers.get(sub);
			if(widgetProvider != null) {
				label = widgetProvider.labelColumn();
				valueWidget = widgetProvider.valueColumn();
				linkButton = widgetProvider.linkColumn();
				if(valueWidget != null) {
					labelWidgets.put(sub, valueWidget);
					alert2.registerDependentWidget(valueWidget);
				}
			}
			
			if(label == null) {
				final Map<OgemaLocale, String> innerMap = labels.get(sub);
				label = getLabel(sub, innerMap, widgetProvider);
			}
			if(valueWidget == null) {
				if(label instanceof Label)
					valueWidget = createValueWidget(sub, type, (Label)label, mh, isEditable(sub));
				else
					valueWidget = createValueWidget(sub, type, null, mh, isEditable(sub));
				if(valueWidget == null) continue;
				if(widgetProvider != null) {
					labelWidgets.put(sub, valueWidget);
					alert2.registerDependentWidget(valueWidget);
				}
			}

			if(linkButton == null) {
				final Map<OgemaLocale, String> linkMap = links.get(sub);
				if((linkMap != null) && (!linkMap.isEmpty())) {
					linkButton = getLinkButton(sub, linkMap, widgetProvider);
				}
			}
			if(triggeringSubs.contains(sub)) valueWidget.registerDependentWidget(alert2);
			performAddEditLine(label, valueWidget, linkButton, sub, type, etb, widgetProvider);
		}
	}
	
	protected void performAddEditLine(OgemaWidget label, OgemaWidget valueWidget, OgemaWidget linkButton,
			String sub, TypeResult type,
			EditTableBuilder etb, EditLineProvider widgetProvider) {
		etb.addEditLine(label, valueWidget, linkButton);		
	}
	
	@Override
	protected void addWidgets() {
		sampleResource = ResourceHelper.getSampleResource(primaryEntryTypeClass());
		sampleResourceLength = sampleResource.getPath().length()+1;
		setData(sampleResource);
		super.addWidgets();
	}
	
	protected OgemaWidget alert2;
	@Override
	protected void registerWidgetsAboveTable() {
		StaticTable pageInfoTable = new StaticTable(1, 2, new int[]{9, 3});
		final Map<OgemaLocale, String> innerMap = labels.get(HEADER_LABEL_ID);
		if((innerMap != null) && (!innerMap.isEmpty())) {
			Alert infoAlert = new Alert(page, "infoAlert", "") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					String text = innerMap.get(req.getLocale());
					if(text == null) text = innerMap.get(localeDefault);
					if(text != null) setText(text, req);
					else setText("", req);
					setWidgetVisibility(true, req);
					setStyle(AlertData.BOOTSTRAP_SUCCESS, req);
					allowDismiss(true, req);
				}
			};
			//Label label = getLabel("_LHeader", innerMap, null);
			pageInfoTable.setContent(0, 0, infoAlert);
		}
		final Map<OgemaLocale, String> linkMap = links.get(HEADER_LINK_ID);
		if((linkMap != null) && (!linkMap.isEmpty())) {
			RedirectButton linkButton = getLinkButton("_Header", linkMap, null);
			pageInfoTable.setContent(0, 1, linkButton);
		}
		page.append(pageInfoTable);
		
		alert2 = new EmptyWidget(page, "alert2") {
			private static final long serialVersionUID = 1L;
			@Override
			//public void updateDependentWidgets(OgemaHttpRequest req) {
			public void onGET(OgemaHttpRequest req) {
				for(Entry<String, EditLineProvider> p: providers.entrySet()) {
					OgemaWidget w = labelWidgets.get(p.getKey());
					if(w == null) continue;
					switch(p.getValue().visibility(ColumnType.VALUE, req)) {
					case HIDDEN:
						w.setWidgetVisibility(false, req);
						break;
					case DISABLED:
						w.setWidgetVisibility(true, req);
						w.disable(req);
						break;
					case ENABLED:
						w.setWidgetVisibility(true, req);
						w.enable(req);
					}
				}
			}
		};
	}
	
	protected String getSubPath(Resource res) {
		return res.getPath().substring(sampleResourceLength);
	}

	protected RedirectButton getLinkButton(String sub, Map<OgemaLocale, String> linkMap, EditLineProvider elp) {
		return new RedirectButton(page, WidgetHelper.getValidWidgetId("linkButton"+sub+pid()), "") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(elp != null) {
					Visibility vis = elp.visibility(ColumnType.LABEL, req);
					if(vis == Visibility.HIDDEN) {
						setWidgetVisibility(false, req);
						return;
					} else {
						setWidgetVisibility(true, req);
						if(vis == Visibility.DISABLED) disable(req);
						else enable(req);
					}
				}
				String text = LINK_BUTTON_TEXTS.get(req.getLocale());
				if(text == null) text = LINK_BUTTON_TEXTS.get(localeDefault);
				if(text != null) setText(text, req);
				else setText("*"+sub+"*", req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String text = linkMap.get(req.getLocale());
				if(text == null) text = linkMap.get(localeDefault);
				if(text != null) setUrl(text, req);
				else setUrl("*"+sub+"*/error.html", req);
			}
		};		
	}
	protected Label getLabel(String sub, Map<OgemaLocale, String> innerMap, EditLineProvider elp) {
		return new Label(page, WidgetHelper.getValidWidgetId("label"+sub+pid())) {
			private static final long serialVersionUID = -2849170377959516221L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(elp != null) {
					Visibility vis = elp.visibility(ColumnType.LABEL, req);
					if(vis == Visibility.HIDDEN) {
						setWidgetVisibility(false, req);
						return;
					} else {
						setWidgetVisibility(true, req);
						if(vis == Visibility.DISABLED) disable(req);
						else enable(req);
					}
				}
				String text = innerMap.get(req.getLocale());
				if(text == null) text = innerMap.get(localeDefault);
				if(text != null) setText(text, req);
				else setText("*"+sub+"*", req);
			}
		};
	}
	
	protected boolean isEditable(String sub) {
		Boolean specific = isEditable.get(sub);
		if(specific == null) return defaultIsEditable;
		else return specific;
	}
	
	protected OgemaWidget createValueWidget(String sub, TypeResult type2,
			Label labelWidgetForValue, ObjectResourceGUIHelperExt mhLoc, boolean isEditable) {
		String subId = WidgetHelper.getValidWidgetId(sub);
		if(StringResource.class.isAssignableFrom(type2.type)) {
			if(isEditable(sub))	return mhLoc.stringEdit(sub, alert);
			else return mhLoc.stringLabel(sub);
		} else if(FloatResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?low:0;
				float upv = (up!=null)?up:999999f;
				TextField valueWidget = mhLoc.floatEdit((String)sub, alert, lowv, upv,
						sub+" limits:"+lowv+" to "+upv);
				valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.floatLabel(sub, "%.2f");
		} else if(BooleanResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				BooleanResourceCheckbox valueWidget = mhLoc.booleanEdit((String)sub);
				valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.resourceLabel(sub, 20);
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
					valueWidget.registerDependentWidget(valueWidget);
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
					valueWidget.registerDependentWidget(valueWidget);
					return valueWidget;
				}
			} else return mhLoc.intLabel(sub, 0);
		} else if(TimeResource.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				final String format;
				if(sub.contains("Day")) format = "YYYY-MM-DD";
				else format = "YYYY-MM-DD HH:mm:ss";
				DatepickerTimeResource valueWidget = mhLoc.datepicker((String)sub, format, (String)null, (String)null);
				valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return mhLoc.timeLabel(sub, 0);
		} else if(ResourceList.class.isAssignableFrom(type2.type)) {
			if(isEditable)	{
				RedirectButton valueWidget = new ResourceOfTypeTableOpenButton(page, "open_"+sub, pid(), exPage, null) {
					private static final long serialVersionUID = 1L;

					@Override
					protected Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData,
							OgemaHttpRequest req) {
						return type2.elementType;
					}
				};
				/*new ResourceTableOpenButton(page, "open_"+sub, pid(), exPage, null) {
					private static final long serialVersionUID = 1L;

					@Override
					protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
							Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
						List<NavigationPublicPageData> provs = appData.systemAccessForPageOpening().
								getPages(primaryEntryTypeClass(), true);
						String url = null;
						for(NavigationPublicPageData p: provs) {
							if((p.typesListedInTable() != null) &&
									p.typesListedInTable().contains(type2.elementType)) {
								url = p.getUrl();
								break;
							}
						}
						//TODO: In this case we have to inject the type into configInfo
						if(url == null) url = SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_PROVIDER);
						return appData.systemAccessForPageOpening().getPageByProvider(url);//super.getPageData(appData, type, typeRequested);
					}
					
					@Override
					protected Object getContext(ExtensionResourceAccessInitData appData, Resource object) {
						return object.getResourceType().getName();
					}
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						super.onGET(req);
						ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
						Resource destination = getResource(appData, req);
						if(destination == null) {
							disable(req);
							setText("--", req);
							return;
						}

						int size = getSize(destination, appData);
						String text = "Open";
						setText(text+"("+size+")", req);
					}
				};*/
				valueWidget.registerDependentWidget(valueWidget);
				return valueWidget;
			} else return new Label(page, "noEdit_"+sub, "Not Allowed");
		} else {
			return null;
		}
	}
}
