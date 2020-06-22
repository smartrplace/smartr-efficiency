package org.smartrplace.gui.filtering;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A generic filter with a fixed list of items*/
public class GenericFilterFixedSingle<T> extends GenericFilterBase<T> {
	protected final T value;
	
	public GenericFilterFixedSingle(T object, Map<OgemaLocale, String> optionLabel) {
		super(optionLabel);
		value = object;
	}
	
	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		return value.equals(object);
	}
	
	public T getValue() {
		return value;
	}
}
