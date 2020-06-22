package org.smartrplace.gui.filtering;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** A generic filter with a fixed list of items*/
public abstract class GenericFilterFixedGroup<T, G> extends GenericFilterBase<T> {
	protected final G group;
	
	public GenericFilterFixedGroup(G group, Map<OgemaLocale, String> optionLabel) {
		super(optionLabel);
		this.group = group;
	}
	
	public G getGroup() {
		return group;
	}
}
