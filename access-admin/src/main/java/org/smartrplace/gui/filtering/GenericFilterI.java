package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.List;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public interface GenericFilterI<T> {
	boolean isInSelection(T object, OgemaHttpRequest req);
	
	/** Overwrite this to improve efficiency, the behaviour should not change*/
	default List<T> getFiltered(List<T> objects, OgemaHttpRequest req) {
		List<T> result = new ArrayList<>();
		for(T obj: objects) {
			if(isInSelection(obj, req))
				result.add(obj);
		}
		return result;
	}
}
