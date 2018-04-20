package org.smartrplace.smarteff.util.editgeneric;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class EditLineProviderDisabling implements EditLineProvider {
	protected abstract boolean enable(OgemaHttpRequest req);
	
	@Override
	public Visibility visibility(ColumnType column, OgemaHttpRequest req) {
		if(enable(req)) return Visibility.ENABLED;
		else return Visibility.DISABLED;
	}
}
