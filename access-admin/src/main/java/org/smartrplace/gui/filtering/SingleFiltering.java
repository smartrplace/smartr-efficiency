package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** Provides a dropdown for filtering, typically for filtering of the objects used to generate table lines.
 *
 * @param <A> attribute type for which the filtering shall take place
 * @param <T> type of object returned as filtering result (typically type of object used in table)
 */
public abstract class SingleFiltering<A, T> extends TemplateDropdown<GenericFilterOption<A>> implements GenericFilterI<T> {
	private static final long serialVersionUID = 1461509059889019498L;

	public static final OgemaLocale[] defaultOrderedLocales = {OgemaLocale.ENGLISH, OgemaLocale.GERMAN,
			OgemaLocale.FRENCH, OgemaLocale.CHINESE};
	
	/** Just provide the label for the language requested. Usage of default English labels
	 * is handled by the base class
	 * @param object
	 * @param locale
	 * @return
	 */
	//protected abstract String getLabel(A attributeOption, OgemaLocale locale);
	
	/** If true then each destination object of type T has only a single base attribute of type A 
	 * (e.g. the room in which a device is located). If false several base attributes apply per
	 * destination object, e.g. the user groups to which a user applies.<br>
	 * If true then {@link #getAttribute(Object)} is used, otherwise {@link #getAttributes(Object)}.
	 * In the class implementation always {@link #getAttributes(Object)} is used as this is more
	 * generic, but this may be changed in the future to improve performance.
	 */
	protected abstract boolean isAttributeSinglePerDestinationObject();
	
	/** Only relevant if options update is configured*/
	protected long getFrameworkTime( ) {return 0;}
	
	/**If null is returned the default options set via {@link #addOption(GenericFilterOption, Map)} etc.
	 * are used. Otherwise only the dynamic options are displayed
	 * 
	 * @param req
	 * @return
	 */
	protected List<GenericFilterOption<A>> getOptionsDynamic(OgemaHttpRequest req) {
		return null;
	}

	protected A getAttribute(T object) {
		throw new IllegalStateException("Either getAttribute or getAttributes must be overriden!");
	};
	protected List<A> getAttributes(T object) {
		List<A> result = new ArrayList<>();
		result.add(getAttribute(object));
		return result ;
	};
	
	public static enum OptionSavingMode {
		//A default is used for each new session
		NONE,
		//A map shall be saved containing the value for each user, if the user has no predefined value use default
		PER_USER,
		//A change in the selection will affect all users
		GENERAL
	}
	protected final OptionSavingMode saveOptionMode;
	protected final long optionSetUpdateRate;
	protected final boolean addAllOption;
	
	public class AllOption implements GenericFilterOption<A> {

		@Override
		public boolean isInSelection(A object, OgemaHttpRequest req) {
			return true;
		}

		@Override
		public Map<OgemaLocale, String> optionLabel() {
			return LocaleHelper.getLabelMap(allOptionsForStandardLocales());
		}
		
	}
	public final AllOption ALL_OPTION = new AllOption();

	protected final Map<String, String> preSelectionPerUser;
	protected String preSelectionGeneralEnglish = null;
	
	protected String[] allOptionsForStandardLocales() {
		return new String[] {"All", "Alle", "Tous", "All"};
	}
	
	protected List<GenericFilterOption<A>> filteringOptions = new ArrayList<>();
	//protected final TemplateDropdown<A> dropdown;
	
	//Do not use directly, use #label for reading and #getKnownLabels for writing
	//protected Map<OgemaLocale, Map<String, GenericFilterOption<A>>> knowLabels = new HashMap<>();
	
	public SingleFiltering(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		this(page, id, saveOptionMode, -1, true);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param saveOptionMode
	 * @param optionSetUpdateRate if negative no updates of filteringOptions will be made. If zero or {@link #getFrameworkTime()} is zero
	 * 		then an update will be triggered on every request. This requires {@link #getOptionsDynamic(OgemaHttpRequest)} to be
	 * 		overridden
	 */
	public SingleFiltering(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			boolean addAllOption) {
		super(page, "SingleFiltDrop"+id);
		this.saveOptionMode = saveOptionMode;
		this.optionSetUpdateRate = optionSetUpdateRate;
		this.addAllOption = addAllOption;
		if(saveOptionMode == OptionSavingMode.PER_USER) {
			preSelectionPerUser = new HashMap<>();
		} else
			preSelectionPerUser = null;
		if(addAllOption) {
			addOption(ALL_OPTION);
			preSelectionGeneralEnglish = LocaleHelper.getLabel(ALL_OPTION.optionLabel(), null);
		}
		//int idx = 0;
		//for(String allLabel: allOptionsForStandardLocales()) {
		//	getKnownLabels(defaultOrderedLocales[idx]).put(allLabel, null);
		//	idx++;
		//}
		setTemplate(new DefaultDisplayTemplate<GenericFilterOption<A>>() {
			@Override
			public String getLabel(GenericFilterOption<A> object, OgemaLocale locale) {
				return label(object, locale);
			}
		});
		//setDefaultAddEmptyOption(true);
	}
	
	//protected void addLabels(GenericFilterOption<A> object, Map<OgemaLocale, String> optionLabel) {
	//	for(Entry<OgemaLocale, String> lab: optionLabel.entrySet()) {
	//		getKnownLabels(lab.getKey()).put(lab.getValue(), object);
	//	}
	//}
	
	/** Add filtering option that selects a single base object
	 * @param optionLabel Just provide the label for the language you want. Usage of default English labels
	 * is handled by the base class. {@link LocaleHelper} usage is recommended.
	 * @return Further options can be added to the result*/
	public GenericFilterFixed<A> addOptionSingle(A object, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(object, optionLabel);
		addOption(result);
		return result;
	}
	/** Add filtering option that selects several base objects*/
	public GenericFilterFixed<A> addOptionSingle(A[] objects, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(objects, optionLabel);
		addOption(result);
		return result;
	}
	/** Add custom-created filtering option<br>
	 * Note that the filtering on this level should not depend on the {@link OgemaHttpRequest} for now.*/
	public void addOption(GenericFilterOption<A> newOption) {
		filteringOptions.add(newOption);
		//addLabels(newOption, optionLabel);
	}
	
	/** Only relevant if no dynamic option setting is configured*/
	public void finishOptionsSetupOnStartup() {
		setDefaultItems(filteringOptions);
		selectDefaultItem(filteringOptions.get(0));
	}
	
	/*protected Map<String, GenericFilterOption<A>> getKnownLabels(OgemaLocale locale) {
		Map<String, GenericFilterOption<A>> result = knowLabels.get(locale==null?OgemaLocale.ENGLISH:locale);
		if(result == null) {
			result = new HashMap<>();
			knowLabels.put(locale, result);
		}
		return result;
	}*/
	
	protected String labelLocaleOnly(GenericFilterOption<A> object, OgemaLocale locale) {
		return object.optionLabel().get(locale);
		/*Map<String, GenericFilterOption<A>> subMap = getKnownLabels(locale);
		for(Entry<String, GenericFilterOption<A>> lab: subMap.entrySet()) {
			if(lab.getValue() == null) {
				if(object == null)
					return lab.getKey();
				continue;
			}
			if(lab.getValue().equals(object))
				return lab.getKey();
		}
		return null;*/
	}
	protected String label(GenericFilterOption<A> object, OgemaLocale locale) {
		return LocaleHelper.getLabel(object.optionLabel(), locale);
		/*if(locale == null)
			locale = OgemaLocale.ENGLISH;
		String result = labelLocaleOnly(object, locale);
		if(result != null) {
			return result;
		} else if(locale != null && locale != OgemaLocale.ENGLISH) {
			result = labelLocaleOnly(object, OgemaLocale.ENGLISH);
			//if(result != null) {
			//	getKnownLabels(OgemaLocale.ENGLISH).put(result, object);
			//}			
		}
		return result;*/
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		if(saveOptionMode == OptionSavingMode.GENERAL) {
			preSelectionGeneralEnglish = LocaleHelper.getLabel(getSelectedItem(req).optionLabel(), null);
			//selectDefaultItem(preSelectionGeneral);
		} else if (saveOptionMode == OptionSavingMode.PER_USER) {
			GenericFilterOption<A> selected = getSelectedItem(req);
			String user = GUIUtilHelper.getUserLoggedIn(req);
			preSelectionPerUser.put(user, LocaleHelper.getLabel(selected.optionLabel(), null));
		}
	}
	
	
	protected long lastOptionsUpdate = -1;
	@Override
	public void onGET(OgemaHttpRequest req) {
		if(optionSetUpdateRate >= 0) {
			long now = getFrameworkTime();
			if(now == 0 || ((now-lastOptionsUpdate) > optionSetUpdateRate)) {
				List<GenericFilterOption<A>> dynOpts = getOptionsDynamic(req);
				if(dynOpts != null) {
					if(addAllOption) {
						filteringOptions = new ArrayList<>();
						filteringOptions.add(ALL_OPTION);
						filteringOptions.addAll(dynOpts);
					} else
						filteringOptions = dynOpts;
					setDefaultItems(filteringOptions);
					GenericFilterOption<A> defaultItem = getFilterOption(preSelectionGeneralEnglish);
					selectDefaultItem(defaultItem);
					update(filteringOptions, defaultItem, req);
				}
				lastOptionsUpdate = now;
			}
		}
		if (saveOptionMode == OptionSavingMode.PER_USER) {
			String user = GUIUtilHelper.getUserLoggedIn(req);
			GenericFilterOption<A> presel = getFilterOption(preSelectionPerUser.get(user));
			selectItem(presel, req);
		}
	}
	
	protected GenericFilterOption<A> getFilterOption(String englishLabel) {
		for(GenericFilterOption<A> item: filteringOptions) {
			if(LocaleHelper.getLabel(item.optionLabel(), null).equals(preSelectionGeneralEnglish)) {
				return item;
			}
		}
		return null;
	}
	
	protected final Map<String, List<T>> destinationObjects = new HashMap<>();
	protected final Map<T, Long> destinationObjectsChecked = new HashMap<>();
	List<T> getDestinationList(GenericFilterOption<A> attribute) {
		String stdLabel = LocaleHelper.getLabel(attribute.optionLabel(), null);
		List<T> alist = destinationObjects.get(stdLabel);
		if(alist == null) {
			alist = new ArrayList<>();
			destinationObjects.put(stdLabel, alist);
		}
		return alist;
	}
	
	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		GenericFilterOption<A> selected = getSelectedItem(req);
		if(selected == null) {
			//empty option = All
			return true;
		}
		List<T> alist = getDestinationList(selected);
		Long lastUpdate = destinationObjectsChecked.get(object);
		long now = getFrameworkTime();
		if(lastUpdate == null ||
				((optionSetUpdateRate >= 0) && ((now-lastOptionsUpdate) > optionSetUpdateRate))) {
			List<A> tlist = getAttributes(object);
			for(GenericFilterOption<A> option: filteringOptions) {
				boolean found = false;
				for(A attr: tlist) {
					if(option.isInSelection(attr, null)) {
						found = true;
						break;
					}
				}
				if(!found)
					continue;
				List<T> alistsub = getDestinationList(option);
				if(!alistsub.contains(object))
					alistsub.add(object);
			}
			destinationObjectsChecked.put(object, now);
		}
		return alist.contains(object);
	}
}
