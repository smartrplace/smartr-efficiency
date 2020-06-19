package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
public abstract class SingleFiltering<A, T> extends TemplateDropdown<GenericFilterOption<A>> implements GenericFilterOption<T> {
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
	protected final Map<String, GenericFilterOption<A>> preSelectionPerUser;
	protected GenericFilterOption<A> preSelectionGeneral;
	
	protected String[] allOptionsForStandardLocales() {
		return new String[] {"All", "Alle", "Tous", "All"};
	}
	
	protected List<GenericFilterOption<A>> filteringOptions = new ArrayList<>();
	//protected final TemplateDropdown<A> dropdown;
	
	//Do not use directly, use #label for reading and #getKnownLabels for writing
	protected Map<OgemaLocale, Map<String, GenericFilterOption<A>>> knowLabels = new HashMap<>();

	public SingleFiltering(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		super(page, "SingleFiltDrop"+id);
		this.saveOptionMode = saveOptionMode;
		if(saveOptionMode == OptionSavingMode.PER_USER) {
			preSelectionPerUser = new HashMap<>();
		} else
			preSelectionPerUser = null;
		int idx = 0;
		for(String allLabel: allOptionsForStandardLocales()) {
			getKnownLabels(defaultOrderedLocales[idx]).put(allLabel, null);
			idx++;
		}
		setTemplate(new DefaultDisplayTemplate<GenericFilterOption<A>>() {
			@Override
			public String getLabel(GenericFilterOption<A> object, OgemaLocale locale) {
				return label(object, locale);
			}
		});
		setDefaultAddEmptyOption(true);
	}
	
	protected void addLabels(GenericFilterOption<A> object, Map<OgemaLocale, String> optionLabel) {
		for(Entry<OgemaLocale, String> lab: optionLabel.entrySet()) {
			getKnownLabels(lab.getKey()).put(lab.getValue(), object);
		}
	}
	
	/** Add filtering option that selects a single base object
	 * @param optionLabel Just provide the label for the language you want. Usage of default English labels
	 * is handled by the base class. {@link LocaleHelper} usage is recommended.
	 * @return Further options can be added to the result*/
	public GenericFilterFixed<A> addOptionSingle(A object, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(object);
		addOption(result, optionLabel);
		return result;
	}
	/** Add filtering option that selects several base objects*/
	public GenericFilterFixed<A> addOptionSingle(A[] objects, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(objects);
		addOption(result, optionLabel);
		//filteringOptions.add(result);
		//addLabels(result, optionLabel);
		return result;
	}
	/** Add custom-created filtering option<br>
	 * Note that the filtering on this level should not depend on the {@link OgemaHttpRequest} for now.*/
	public void addOption(GenericFilterOption<A> newOption, Map<OgemaLocale, String> optionLabel) {
		filteringOptions.add(newOption);
		addLabels(newOption, optionLabel);
	}
	
	public void finishOptionsSetup() {
		setDefaultItems(filteringOptions);
		selectDefaultItem(filteringOptions.get(0));
	}
	
	protected Map<String, GenericFilterOption<A>> getKnownLabels(OgemaLocale locale) {
		Map<String, GenericFilterOption<A>> result = knowLabels.get(locale==null?OgemaLocale.ENGLISH:locale);
		if(result == null) {
			result = new HashMap<>();
			knowLabels.put(locale, result);
		}
		return result;
	}
	
	protected String labelLocaleOnly(GenericFilterOption<A> object, OgemaLocale locale) {
		Map<String, GenericFilterOption<A>> subMap = getKnownLabels(locale);
		for(Entry<String, GenericFilterOption<A>> lab: subMap.entrySet()) {
			if(lab.getValue() == null) {
				if(object == null)
					return lab.getKey();
				continue;
			}
			if(lab.getValue().equals(object))
				return lab.getKey();
		}
		return null;
	}
	protected String label(GenericFilterOption<A> object, OgemaLocale locale) {
		if(locale == null)
			locale = OgemaLocale.ENGLISH;
		String result = labelLocaleOnly(object, locale);
		if(result != null) {
			return result;
		} else if(locale != null && locale != OgemaLocale.ENGLISH) {
			result = labelLocaleOnly(object, OgemaLocale.ENGLISH);
			if(result != null) {
				getKnownLabels(OgemaLocale.ENGLISH).put(result, object);
			}			
		}
		return result;
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		if(saveOptionMode == OptionSavingMode.GENERAL) {
			preSelectionGeneral = getSelectedItem(req);
			selectDefaultItem(preSelectionGeneral);
		} else if (saveOptionMode == OptionSavingMode.PER_USER) {
			GenericFilterOption<A> selected = getSelectedItem(req);
			String user = GUIUtilHelper.getUserLoggedIn(req);
			preSelectionPerUser.put(user, selected);
		}
	}
	@Override
	public void onGET(OgemaHttpRequest req) {
		if (saveOptionMode == OptionSavingMode.PER_USER) {
			String user = GUIUtilHelper.getUserLoggedIn(req);
			GenericFilterOption<A> presel = preSelectionPerUser.get(user);
			selectItem(presel, req);
		}
	}
	
	protected final Map<GenericFilterOption<A>, List<T>> destinationObjects = new HashMap<>();
	protected final Set<T> destinationObjectsChecked = new HashSet<>();
	List<T> getDestinationList(GenericFilterOption<A> attribute) {
		List<T> alist = destinationObjects.get(attribute);
		if(alist == null) {
			alist = new ArrayList<>();
			destinationObjects.put(attribute, alist);
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
		if(!destinationObjectsChecked.contains(object)) {
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
			destinationObjectsChecked.add(object);
		}
		return alist.contains(object);
	}
	

}
