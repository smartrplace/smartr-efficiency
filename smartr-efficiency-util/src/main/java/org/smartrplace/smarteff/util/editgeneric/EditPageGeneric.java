package org.smartrplace.smarteff.util.editgeneric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.array.BooleanArrayResource;
import org.ogema.core.model.array.ByteArrayResource;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.array.IntegerArrayResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider.ColumnType;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider.Visibility;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericTableWidgetProvider.CapabilityDeclaration;
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
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Label;

/** Template for edit pages based on {@link EditPageBase}. See documentation there.
 * You can specify a header and a general documentation link for the page using
 * {@link #setHeaderLabel(OgemaLocale, String)} and {@link #setHeaderLink(OgemaLocale, String)}.
 * In the methods {@link #setLabel(String, OgemaLocale, String)} etc. you can use any uniqueID instead
 * of the resourceName for lines that do not refer to a single subresource. Such lines should either
 * provider a value widget via {@link EditLineProvider#valueColumn()} or the sub should be processed
 * by an {@link EditPageGenericTableWidgetProvider}.<br>
 * IDs starting with a hash (#) are not processed,
 * but given to the EditPageGenericTableWidgetProviders directly. See {@link DefaultWidgetProvider}
 * for default IDs supported there. 
 * @param <T> resource type to edit
 */
public abstract class EditPageGeneric<T extends Resource> extends EditPageBase<T> {
	public static OgemaLocale EN = OgemaLocale.ENGLISH;
	public static OgemaLocale DE = OgemaLocale.GERMAN;
	public static OgemaLocale FR = OgemaLocale.FRENCH;
	public static OgemaLocale CN = OgemaLocale.CHINESE;
	public static OgemaLocale FORMAT = new OgemaLocale(Locale.TRADITIONAL_CHINESE);

	protected static final String HEADER_LABEL_ID = "#L";
	protected static final String HEADER_LINK_ID = "#H";

	private T sampleResource;
	private int sampleResourceLength;
	private final boolean forceSubResName;
	private final boolean setDefaults;

	/** In this method the page data shall be provided with methods like getLabel
	 * @param sr virtual resource that shall be used to provide paths
	 */
	public abstract void setData(T sr);
	/** If the {@link #primaryEntryTypeClass()} has a different super type from BuildingData and the
	 * primary type registration shall be done automatically overwrite this. The primary type
	 * will be registered as {@link Cardinality.MULTIPLE_OPTIONAL}.
	 */
	public Class<? extends SmartEffResource> getPrimarySuperType() {return null;};
	
	Map<String, Float> lowerLimits = new HashMap<>();
	Map<String, Float> upperLimits = new HashMap<>();
	Map<String, Map<OgemaLocale, Map<String, String>>> displayOptions = new HashMap<>();
	Map<String, Object> widgetContexts = new HashMap<>();
	
	Map<String, EditLineProvider> providers = new HashMap<>();
	//Widgets that are governed by an EditLineProvider
	Map<String, OgemaWidget> labelWidgets = new HashMap<>();
	//Subs that shall trigger alert2 that triggers all widgets
	List<String> triggeringSubs = new ArrayList<>();
	
	protected boolean defaultIsEditable = true;
	Map<String, Boolean> isEditable = new HashMap<>();
	final List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders;
	
	private SubTypeHandler typeHandler;
	
	/**Overwrite in inherited classes to check / add to settings made by the implementing classes
	 * via {@link #setData(Resource)}
	 */
	protected void checkSetData() {}

	/** Overwrite in inherited classes to show button "Fill empty resources"
	 */
	protected boolean showFillEmptyResButton() {
		return false;
	}

	
	public EditPageGeneric() {
		this(null);
	}
	public EditPageGeneric(boolean forceSubResName) {
		this(null, forceSubResName, true);
	}
	public EditPageGeneric(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders) {
		this(additionalWidgetProviders, true, true);
	}
	public EditPageGeneric(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders,
			boolean forceSubResName) {
		this(additionalWidgetProviders, forceSubResName, true);
	}
	public EditPageGeneric(List<EditPageGenericTableWidgetProvider<T>> additionalWidgetProviders,
			boolean forceSubResName, boolean setDefaults) {
		super();
		this.additionalWidgetProviders = additionalWidgetProviders;
		this.forceSubResName = forceSubResName;
		this.setDefaults = setDefaults;
	}

	@Override //make public
	public abstract Class<? extends Resource> primaryEntryTypeClass();

	/*@SuppressWarnings("unchecked")
	private void fillMap(Map<String, Class<? extends Resource>> typeMap, Class<? extends Resource> resType) {
		//typeMap.put("name", StringResource.class);
		for(Method m: resType.getMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(Resource.class.isAssignableFrom(rawClass) && (m.getParameterCount()==0)) {
				typeMap.put(m.getName(), (Class<? extends Resource>) rawClass);				
			}
		}
		for(Class<? extends Resource> rawClass: appManExt.getSubTypes(resType)) {
			ExtensionResourceTypeDeclaration<? extends Resource> decl = appManExt.getTypeDeclaration(rawClass);
			String name = CapabilityHelper.getSingleResourceName(rawClass);
			if(SPPageUtil.isMulti(decl.cardinality()))
				typeMap.put(name, ResourceList.class);
			else
				typeMap.put(name, rawClass);
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
	}*/
	public static class TypeResult {
		/** Only one of type or typeString shall be non-null. Usually typeString should start with
		 * a hash (#).
		 * TODO: The typeString equals sub and this is a doublet information currently. We still have
		 * 		to create a TypeResult object to identify that a line shall be created.
		 */
		public final Class<? extends Resource> type;
		public final String typeString;
		public TypeResult(Class<? extends Resource> type) {
			this.type = type;
			this.typeString = null;
		}
		public TypeResult(String subPath) {
			this.type = null;
			this.typeString = subPath;
		}
		public Class<? extends Resource> elementType = null;
	}

	/*protected TypeResult getType(String subPath) {
		if(subPath.startsWith("#")) {
			return new TypeResult(subPath);
		}
		String[] els = subPath.split("/");
		Class<? extends Resource> cr = primaryEntryTypeClass();
		for(int i=0; i<=els.length; i++) {
			if(cr == null)
				return null;
			if(i == els.length) {
				if(ResourceList.class.isAssignableFrom(cr)) {
					TypeResult result = new TypeResult(cr);
					ExtensionResourceTypeDeclaration<?> superType = CapabilityHelper.getTypeFromName(els[i-1], appManExt);
					result.elementType = superType.dataType();
					return result;
				}
				return new TypeResult(cr);
			}
			//we cannot go over ValueResources and ResourceLists here
			if(els[i].contains("#$")) {
				int inStringIdx = els[i].indexOf("#$");
				//String intString = els[i].substring(inStringIdx+2);
				String realSub = els[i].substring(0, inStringIdx);
				//int index = Integer.parseInt(intString);
				cr = getElementClassOfResourceList(cr, realSub);
				if(cr == null)
					throw new IllegalStateException("Path "+subPath+" does not reference ResourceList or could not be processed at "+els[i]+"!");
				continue;
			}
			if(ValueResource.class.isAssignableFrom(cr)) throw new IllegalStateException("Path "+subPath+" includes a ValueResouce in middle!");
			if(ResourceList.class.isAssignableFrom(cr)) {
				throw new IllegalStateException("Path "+subPath+" includes a ResourceList in middle!");
			}
			cr = getSubTypes(cr).get(els[i]);
		}
		//if(els.length == 1) return new TypeResult(cr);
		throw new IllegalStateException("we should never get here");		
	}
	
	@SuppressWarnings("unchecked")
	public static Class<? extends Resource> getElementClassOfResourceList(
			Class<? extends Resource> parentClass, String resListName) {
		for(Method m: parentClass.getMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(ResourceList.class.isAssignableFrom(rawClass) && (m.getParameterCount()==0)) {
				//Analyse this;
				if(m.getName().equals(resListName)) {
					//getSubType
					Type genClass = m.getGenericReturnType();
					if(genClass instanceof ParameterizedType) {
						Type[] types = ((ParameterizedType)genClass).getActualTypeArguments();
						if(types.length == 1) {
							if(types[0] instanceof Class) {
								Class<?> cl = (Class<?>)(types[0]);
								if(Resource.class.isAssignableFrom(cl))
									return (Class<? extends Resource>) cl;
							}
						}
					}
					return null;
				}
			}
		}
		return null;
		
	}*/
	
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

	/** If a unit for res is found, append it to the label, otherwise just set normal label. */
	protected void setLabelWithUnit(Resource res, OgemaLocale locale, String text) {
		String unit = "";
		if(res instanceof TemperatureResource)
			unit = "°C";
		else if(res instanceof PhysicalUnitResource)
			unit = ((PhysicalUnitResource) res).getUnit().toString();
		
		String unitAppend = "";
		if(!unit.isEmpty())
			unitAppend = " (" + unit + ")";
		setLabel(res, locale, text + unitAppend);
	}
	/** If a unit for res is found, append it to the label, otherwise just set normal label. */
	protected void setLabelWithUnit(Resource res, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setLabelWithUnit(res, locale, text);
		setLabelWithUnit(res, locale2, text2);
	}
	/** If a unit for res is found, append it to the label, otherwise just set normal label. */
	protected void setLabelWithUnit(Resource res, OgemaLocale locale, String text, float min, float max) {
		setLabelWithUnit(res, locale, text);
		setLimits(res, min, max);
	}
	/** If a unit for res is found, append it to the label, otherwise just set normal label. */
	protected void setLabelWithUnit(Resource res, OgemaLocale locale, String text, OgemaLocale locale2, String text2,
			float min, float max) {
		setLabelWithUnit(res, locale, text);
		setLabelWithUnit(res, locale2, text2);
		setLimits(res, min, max);
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
	protected void addValueWidget(Resource res, OgemaWidget valueWidget) {
		EditLineProvider prov = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return valueWidget;
			}
		};
		setLineProvider(getSubPath(res), prov);
	}
	protected void addLabelValueWidget(Resource res, OgemaWidget labelWidget, OgemaWidget valueWidget) {
		EditLineProvider prov = new EditLineProvider() {
			@Override
			public OgemaWidget labelColumn() {
				return labelWidget;
			}
			
			@Override
			public OgemaWidget valueColumn() {
				return valueWidget;
			}
		};
		setLineProvider(getSubPath(res), prov);
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
	protected void setWidgetContext(String resourceName, Object context) {
		widgetContexts.put(resourceName, context);		
	}
	protected void setWidgetContext(Resource res, Object context) {
		setWidgetContext(getSubPath(res), context);
	}
	
	protected void setTriggering(String resourceName) {
		triggeringSubs.add(resourceName);
	}
	protected void setTriggering(Resource res) {
		setTriggering(getSubPath(res));
	}
	
	protected void setEditable(String resourceName, boolean editable) {
		isEditable.put(resourceName, editable);
	}
	protected void setEditable(Resource res, boolean editable) {
		setEditable(getSubPath(res), editable);
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
		if(forceSubResName) {
			Resource name = res.getSubResource("name");
			if ((name != null) && (name instanceof StringResource)) {
				ValueResourceHelper.setIfNew((StringResource)name, newName);
			}
		}
		if(setDefaults)
			setDefaults(res, DefaultSetModes.SET_IF_NEW);
		//Map<String, Class<? extends Resource>> types = getTypes();
		for(String sub: labels.keySet()) {
			TypeResult type = typeHandler.getType(sub);
			if(type == null || type.type == null) continue;
			if(FloatResource.class.isAssignableFrom(type.type)) {
				float val;
				FloatResource valRes = ResourceHelper.getSubResource(res, sub);
				if (valRes instanceof TemperatureResource)
					val = ((TemperatureResource) valRes).getCelsius();
				else
					val = valRes.getValue();
				return checkLimits(lowerLimits.get(sub), upperLimits.get(sub), val);
				/*Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?low:0;
				float upv = (up!=null)?up:999999f;
				FloatResource valRes = ResourceHelper.getSubResource(res, sub);
				float val;
				if (valRes instanceof TemperatureResource)
					val = ((TemperatureResource) valRes).getCelsius();
				else
					val = valRes.getValue();
				if(Float.isNaN(val) || (val < lowv)||(val > upv)) {
					return false;
				}*/
			}
		}
		return true;
	}
	
	public static boolean checkLimits(Float low, Float up, float val) {
		float lowv = (low!=null)?low:0;
		float upv = (up!=null)?up:999999f;
		if(Float.isNaN(val) || (val < lowv)||(val > upv)) {
			return false;
		}
		return true;
	}
	
	@Override
	protected void getEditTableLines(EditTableBuilder etb) {
		//Map<String, Class<? extends Resource>> types = getTypes();
		//TODO: Not clear why this is set to false => we change for testing. This may cause trouble with upload buttons
		mh.setDoRegisterDependentWidgets(true);
		for(String sub: labels.keySet()) {
			if(sub.equals(HEADER_LABEL_ID) || sub.equals(HEADER_LINK_ID)) continue;
			OgemaWidget valueWidget = null;
			OgemaWidget label = null;
			OgemaWidget linkButton = null;
			TypeResult type = null;
			try {
				type = typeHandler.getType(sub);
			} catch(NullPointerException e) {
				throw new IllegalStateException("Could not process sub "+sub+"! Note that ResourceList names must fit the elementType name.\r\nPlease check if the resource type has been registed in resourcesDefined as dependency.", e);
			}
			if(type == null) {
				Resource sampleDest = ResourceHelper.getSubResource(sampleResource, sub);
				if(sampleDest == null)
					continue;
				type = new TypeResult(sampleDest.getResourceType());
			}
			
			EditLineProvider widgetProvider = providers.get(sub);
			if(widgetProvider != null) {
				label = widgetProvider.labelColumn();
				valueWidget = widgetProvider.valueColumn();
				linkButton = widgetProvider.linkColumn();
				if(valueWidget != null) {
					labelWidgets.put(sub, valueWidget);
					mh.triggerOnPost(alert2, valueWidget); //alert2.registerDependentWidget(valueWidget);
				}
			}
			
			if(label == null) {
				final Map<OgemaLocale, String> innerMap = labels.get(sub);
				label = getLabel(sub, innerMap, widgetProvider);
			}
			if(valueWidget == null) {
				if(label instanceof Label)
					valueWidget = createValueWidget(sub, type, (Label)label, mh, isEditable(sub), null);
				else
					valueWidget = createValueWidget(sub, type, null, mh, isEditable(sub), null);
				if(valueWidget == null) continue;
				if(widgetProvider != null) {
					labelWidgets.put(sub, valueWidget);
					mh.triggerOnPost(alert2, valueWidget); //alert2.registerDependentWidget(valueWidget);
				}
			}

			if(linkButton == null) {
				final Map<OgemaLocale, String> linkMap = links.get(sub);
				if((linkMap != null) && (!linkMap.isEmpty())) {
					linkButton = getLinkButton(sub, linkMap, widgetProvider);
				}
			}
			if(triggeringSubs.contains(sub)) mh.triggerOnPost(alert2, valueWidget); //valueWidget.registerDependentWidget(alert2);
			performAddEditLine(label, valueWidget, linkButton, sub, type, etb, widgetProvider);
		}
	}
	
	protected void performAddEditLine(OgemaWidget label, OgemaWidget valueWidget, OgemaWidget linkButton,
			String sub, TypeResult type,
			EditTableBuilder etb, EditLineProvider widgetProvider) {
		etb.addEditLine(label, valueWidget, linkButton);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void addWidgets() {
		typeHandler = new SubTypeHandler(primaryEntryTypeClass(), appManExt);
		sampleResource = (T) ResourceHelper.getSampleResource(primaryEntryTypeClass());
		sampleResourceLength = sampleResource.getPath().length()+1;
		alert = new Alert(page, "alert"+pid(), "");
		setData(sampleResource);
		checkSetData();
		super.addWidgets();
		if(showFillEmptyResButton()) {
			Button setDefaults = createDefaultsButton(DefaultSetModes.SET_IF_NEW, "Fill empty resources");
			page.append(setDefaults);
		}
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
			RedirectButton linkButton = getLinkButton("_Header", linkMap, (EditLineProvider)null);
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
	
	/** Slightly extended version of {@link #getLabel(String, Map, Visibility)}*/
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

	
	private boolean isEditable(String sub) {
		Boolean specific = isEditable.get(sub);
		if(specific == null) return defaultIsEditable;
		else return specific;
	}
	
	protected EditPageGenericTableWidgetProvider<T> defaultWP =
			new DefaultWidgetProvider<T>();
	protected OgemaWidget createValueWidget(String sub, TypeResult type2,
			Label labelWidgetForValue, ObjectResourceGUIHelperExt mhLoc, boolean isEditable, String pidColId) {
		Map<Integer, EditPageGenericTableWidgetProvider<T>> fittingProvs = new HashMap<>();
		if(additionalWidgetProviders != null) for(EditPageGenericTableWidgetProvider<T> wp: additionalWidgetProviders) {
			for(CapabilityDeclaration cap: wp.capabilities()) {
				if((type2.typeString != null) && (cap.typeString != null) &&
						cap.typeString.equals(type2.typeString)) {
					fittingProvs.put(cap.priority, wp);
				} else if((type2.type == null) || (cap.type == null)) {
					continue;
				} else if(cap.type.isAssignableFrom(type2.type)) {
					//TODO handle same priority
					fittingProvs.put(cap.priority, wp);
				} else if(ResourceList.class.isAssignableFrom(type2.type) &&
						cap.type.isAssignableFrom(type2.elementType)) {
					fittingProvs.put(cap.priority, wp);					
				}
			}
		}
		if(fittingProvs.isEmpty()) {
			//TODO: Do not always initialize
			defaultWP.setGlobalData(mh, alert, lowerLimits, upperLimits, displayOptions, appManExt, exPage, page,
					primaryEntryTypeClass(), widgetContexts);
			return defaultWP.createValueWidget(sub, type2, labelWidgetForValue, mhLoc, isEditable,
					isEditable(sub), pidColId==null?pid():pid()+pidColId, labels);
		} else {
			Integer maxPriority = Collections.max(fittingProvs.keySet());
			EditPageGenericTableWidgetProvider<T> wp = fittingProvs.get(maxPriority);
			wp.setGlobalData(mh, alert, lowerLimits, upperLimits, displayOptions, appManExt, exPage, page,
					primaryEntryTypeClass(), widgetContexts);
			return wp.createValueWidget(sub, type2, labelWidgetForValue, mhLoc, isEditable,
					isEditable(sub), pid(), labels);
		}
	}
	
	/** Modes/settings for how defaults are set */
	public enum DefaultSetModes {
		/** Only set defaults if the parent resource fails checkResource() */
		PARENT_CHECK,
		/** Set defaults for empty/non-existent resources even if parent passes checks */
		SET_IF_NEW,
		/** Set defaults for all resources, overwriting them. */
		OVERWRITE,
	}
	/**
	 * Set a single default value.  Essentially a generic version of
	 * ValueResourceHelper.setIfNew().
	 * @param res Temperature-, Float-, Integer-, Boolean-, or StringResource.
	 * @param val Value of the resource. Celsius for TemperatureResources.
	 * @return true if resource was created and value was written, false also
	 * for invalid types.
	 */
	protected static <K extends Resource, V> boolean setIfNew(K res, V val) {
		if(!res.exists()) {
			res.create();
			return setAndActivate(res, val);
		} else {
			return false;
		}
	}
	
	/**
	 * (Attempt to) set the value for a generic resource and
	 * activates the resource if a value was written.
	 * @param res
	 * @param val
	 * @return true if value was written
	 */
	protected static <K extends Resource, V> boolean setAndActivate(K res, V val) {
		boolean valueWritten;
		if(res instanceof ValueResource)
			valueWritten = setValue((ValueResource) res, val); //ValueResourceUtils.setValue((ValueResource) res, val);
		else valueWritten = false;
		if (valueWritten)
			res.activate(true);
		return valueWritten;
	}
	
	/** TODO: Remove this as soon as {@link ValueResourceUtils#setValue(ValueResource, Object)} provides
	 * return value
	 * (Attempt to) set the value for a generic resource
	 * @param res
	 * @param val
	 * @return true if value was written
	 */
	@SuppressWarnings("unchecked")
	public static boolean setValue(ValueResource resource, Object value) throws ClassCastException, NumberFormatException {
		if (resource instanceof SingleValueResource) 
			return setValue((SingleValueResource) resource, value.toString());
		else if (resource instanceof Schedule) {
			Collection<SampledValue> values;
			if (value instanceof ReadOnlyTimeSeries)
				values = ((ReadOnlyTimeSeries) value).getValues(Long.MIN_VALUE);
			else if (value instanceof List) 
				values = (Collection<SampledValue>) value;
			else 
				throw new IllegalArgumentException("Schedule value must be either a time series or a collection of SampledValue objects");
			((Schedule) resource).replaceValues(Long.MIN_VALUE, Long.MAX_VALUE, values);
			return true;
		}
		else if (resource instanceof IntegerArrayResource) 
			return ((IntegerArrayResource) resource).setValues((int[]) value);
		else if (resource instanceof FloatArrayResource) 
			return ((FloatArrayResource) resource).setValues((float[]) value);
		else if (resource instanceof TimeArrayResource)
			return ((TimeArrayResource) resource).setValues((long[]) value);
		else if (resource instanceof BooleanArrayResource)
			return ((BooleanArrayResource) resource).setValues((boolean[]) value);
		else if (resource instanceof StringArrayResource)
			return ((StringArrayResource) resource).setValues((String[]) value);
		else if (resource instanceof ByteArrayResource)
			return ((ByteArrayResource) resource).setValues((byte[]) value);
		else if (resource instanceof org.ogema.core.model.simple.OpaqueResource)
			return ((org.ogema.core.model.simple.OpaqueResource) resource).setValue((byte[]) value);
		return false;
	}
	protected static boolean setValue(SingleValueResource resource, String value) throws NumberFormatException {
		if (resource instanceof StringResource) {
			return ((StringResource) resource).setValue(value);
		}
		else if (resource instanceof FloatResource) {
			return ((FloatResource) resource).setValue(Float.parseFloat(value));
		}
		else if (resource instanceof IntegerResource) {
			return ((IntegerResource) resource).setValue(Integer.parseInt(value));
		}
		else if (resource instanceof BooleanResource) {
			return ((BooleanResource) resource).setValue(Boolean.parseBoolean(value));
		}
		else if (resource instanceof TimeResource) {
			return ((TimeResource) resource).setValue(Long.parseLong(value));
		}
		return false;
	}

	/*protected static <K extends Resource, V> boolean setOverwrite(K res, V val) {
		if (res instanceof TemperatureResource) {
			return ((TemperatureResource) res).setCelsius((float) val);
		} else if (res instanceof FloatResource)
			return ((FloatResource) res).setValue((float) val);
		else if (res instanceof IntegerResource)
			return ((IntegerResource) res).setValue((int) val);
		else if (res instanceof BooleanResource)
			return ((BooleanResource) res).setValue((boolean) val);
		else if (res instanceof StringResource)
			return ((StringResource) res).setValue((String) val);
		return false;
	}*/
	
	/**
	 * Set default values according to mode.
	 * @param res
	 * @param mode
	 */
	protected void setDefaults(T res, DefaultSetModes mode) {
		if (mode == DefaultSetModes.PARENT_CHECK) {
			if (res.isActive() == checkResource(res))
				return;
		}
		defaultValues(res, mode);
	}
	
	/**
	 * Override this in the specific edit page and call
	 * {@link #setDefault(Resource, Object, DefaultSetModes)}
	 * on each of the resources you want to set defaults for.
	 * @param res
	 * @param mode
	 */
	protected void defaultValues(T res, DefaultSetModes mode) {}
	
	/**
	 * Set a single default value according to the mode.
	 * Set value if its new, unless mode is OVERWRITE.
	 * @param res
	 * @param val
	 * @param mode
	 * @return true if value was written
	 */
	public static <K extends Resource, V> boolean setDefault(K res, V val, DefaultSetModes mode) {
		if (mode == DefaultSetModes.OVERWRITE) {
			if(!res.exists()) res.create();
			return setValue((ValueResource) res, val);
		} else
			return setIfNew(res, val);
	}
	
	/**
	 * Create a "Set Defaults" button that can be appended to the specific edit page
	 * @param mode
	 * @param label
	 * @return
	 */
	protected Button createDefaultsButton(DefaultSetModes mode, String label) {
		Button btn = new Button(page, "setDefaultsButton_" + mode.toString(), label) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				T res = getReqData(req);
				setDefaults(res, mode);
				// TODO trigger refresh!
			};
		};
		return btn;
	}
	
	public static String getLabel(String sub, Resource res, OgemaLocale locale, Map<String, Map<OgemaLocale, String>>  labels) {
		if (labels != null && labels.containsKey(sub)) {
			Map<OgemaLocale, String> resLabel = labels.get(sub);
			if (resLabel.containsKey(locale)) {
				return resLabel.get(locale);
			} else if (resLabel.containsKey(OgemaLocale.ENGLISH)) {
				return resLabel.get(OgemaLocale.ENGLISH);
			}
		}
		if(res != null) return ResourceUtils.getHumanReadableShortName(res);
		return sub;
	}
}
