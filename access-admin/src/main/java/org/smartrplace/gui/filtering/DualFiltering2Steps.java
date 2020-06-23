package org.smartrplace.gui.filtering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** 
 * 
 * @author dnestle
 *
 * @param <A>
 * @param <G> type of groups in first dropdown
 * @param <T>
 */
public abstract class DualFiltering2Steps<A, G, T> extends SingleFiltering<A, T> {
	private static final long serialVersionUID = 1L;
	protected final SingleFiltering<G, A> firstDropDown;
	
	//protected abstract List<G> getAllGroups(OgemaHttpRequest req);
	//protected abstract List<A> elementsInGroup(G group, OgemaHttpRequest req);
	protected abstract List<GenericFilterOption<A>> getOptionsDynamic(G group, OgemaHttpRequest req);
	protected abstract List<GenericFilterFixedSingle<G>> getGroupOptionsDynamic(OgemaHttpRequest req);
	protected abstract List<G> getGroups(A object);
	
	//We do not support prelection per user here yet*/
	protected final Map<String, String> preSelectionPerGroup = new HashMap<>();
	protected String preSelectionGroup = null;

	public DualFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		this(page, id, saveOptionMode, -1, true);
	}

	public DualFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			boolean addAllOption) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOption);
		if(saveOptionMode == OptionSavingMode.PER_USER)
			throw new UnsupportedOperationException("PER_USER not supported for DualFiltering2Steps");
		//FIXME: For now we add the all option to the first dropdown always. This should be configurable in the future,
		//maybe also dynamically to detect whether this leads to too large dropdowns
		firstDropDown = new SingleFiltering<G, A>(page, id+"_first", saveOptionMode, optionSetUpdateRate, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isAttributeSinglePerDestinationObject() {
				return false;
			}
			
			@Override
			protected List<G> getAttributes(A object) {
				return getGroups(object);
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			protected List<GenericFilterOption<G>> getOptionsDynamic(OgemaHttpRequest req) {
				return (List)(getGroupOptionsDynamic(req));
			}
			
			@Override
			protected long getFrameworkTime() {
				return DualFiltering2Steps.this.getFrameworkTime();
			}
		};
		firstDropDown.registerDependentWidget(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<GenericFilterOption<A>> getOptionsDynamic(OgemaHttpRequest req) {
		GenericFilterOption<G> groupFilterSelected = firstDropDown.getSelectedItem(req);
		if(groupFilterSelected == firstDropDown.ALL_OPTION || groupFilterSelected == null)
			return getOptionsDynamic(null, req);
		if(!(groupFilterSelected instanceof GenericFilterFixedGroup))
			throw new IllegalStateException();
		return getOptionsDynamic(((GenericFilterFixedGroup<A, G>)groupFilterSelected).getGroup(), req);
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return false;
	}
	
	public SingleFiltering<G, A> getFirstDropdown() {
		return firstDropDown;
	}
}
