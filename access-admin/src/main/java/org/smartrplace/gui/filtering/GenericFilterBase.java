package org.smartrplace.gui.filtering;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** A generic filter with a fixed list of items*/
public abstract class GenericFilterBase<T> implements GenericFilterOption<T> {
	protected final Map<OgemaLocale, String> optionLabel;
	
	public GenericFilterBase(Map<OgemaLocale, String> optionLabel) {
		this.optionLabel = optionLabel;
	}
	
	@Override
	public Map<OgemaLocale, String> optionLabel() {
		return optionLabel;
	}
}
