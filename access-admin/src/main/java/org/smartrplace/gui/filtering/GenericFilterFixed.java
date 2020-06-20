package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A generic filter with a fixed list of items*/
public class GenericFilterFixed<T> implements GenericFilterOption<T> {
	protected final List<T> baseOptions = new ArrayList<>();
	protected final Map<OgemaLocale, String> optionLabel;
	
	public GenericFilterFixed(T object, Map<OgemaLocale, String> optionLabel) {
		baseOptions.add(object);
		this.optionLabel = optionLabel;
	}
	public GenericFilterFixed(List<T> objects, Map<OgemaLocale, String> optionLabel) {
		baseOptions.addAll(objects);
		this.optionLabel = optionLabel;
	}
	public GenericFilterFixed(T[] objects, Map<OgemaLocale, String> optionLabel) {
		baseOptions.addAll(Arrays.asList(objects));
		this.optionLabel = optionLabel;
	}
	public GenericFilterFixed(GenericFilterFixed<T> base, Map<OgemaLocale, String> optionLabel) {
		baseOptions.addAll(base.baseOptions);
		this.optionLabel = optionLabel;
	}
	public GenericFilterFixed(GenericFilterFixed<T> base, T object, Map<OgemaLocale, String> optionLabel) {
		baseOptions.addAll(base.baseOptions);
		baseOptions.add(object);
		this.optionLabel = optionLabel;
	}
	public GenericFilterFixed(GenericFilterFixed<T> base, T[] objects, Map<OgemaLocale, String> optionLabel) {
		baseOptions.addAll(base.baseOptions);
		baseOptions.addAll(Arrays.asList(objects));
		this.optionLabel = optionLabel;
	}
	
	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		return baseOptions.contains(object);
	}
	@Override
	public Map<OgemaLocale, String> optionLabel() {
		return optionLabel;
	}
}
