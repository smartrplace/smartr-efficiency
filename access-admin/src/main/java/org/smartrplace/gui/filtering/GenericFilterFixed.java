package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A generic filter with a fixed list of items*/
public class GenericFilterFixed<T> implements GenericFilter<T> {
	protected List<T> baseOptions = new ArrayList<>();
	public GenericFilterFixed(T object) {
		baseOptions.add(object);
	}
	public GenericFilterFixed(List<T> objects) {
		baseOptions.addAll(objects);
	}
	public GenericFilterFixed(T[] objects) {
		baseOptions.addAll(Arrays.asList(objects));
	}
	public GenericFilterFixed(GenericFilterFixed<T> base) {
		baseOptions.addAll(base.baseOptions);
	}
	public GenericFilterFixed(GenericFilterFixed<T> base, T object) {
		baseOptions.addAll(base.baseOptions);
		baseOptions.add(object);
	}
	public GenericFilterFixed(GenericFilterFixed<T> base, T[] objects) {
		baseOptions.addAll(base.baseOptions);
		baseOptions.addAll(Arrays.asList(objects));
	}
	
	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		return baseOptions.contains(object);
	}
}
