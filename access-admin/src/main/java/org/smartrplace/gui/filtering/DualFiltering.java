package org.smartrplace.gui.filtering;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class DualFiltering<A, B, T> implements GenericFilterI<T> {
	protected final SingleFiltering<A, T> singleFilterA;
	protected final SingleFiltering<B, T> singleFilterB;
	
	public DualFiltering(SingleFiltering<A, T> singleFilterA, SingleFiltering<B, T> singleFilterB) {
		this.singleFilterA = singleFilterA;
		this.singleFilterB = singleFilterB;
	}

	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		//boolean inA = singleFilterA.isInSelection(object, req);
		//boolean inB = singleFilterB.isInSelection(object, req);
		//return inA && inB;
		return singleFilterA.isInSelection(object, req) && singleFilterB.isInSelection(object, req);
	}

	public SingleFiltering<A, T> getDropdownA() {
		return singleFilterA;
	}
	public SingleFiltering<B, T> getDropdownB() {
		return singleFilterB;
	}
}
