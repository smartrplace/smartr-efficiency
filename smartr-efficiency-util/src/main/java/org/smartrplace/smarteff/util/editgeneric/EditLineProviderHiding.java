package org.smartrplace.smarteff.util.editgeneric;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class EditLineProviderHiding implements EditLineProvider {
	protected abstract boolean show(OgemaHttpRequest req);
	
	@Override
	public Visibility visibility(ColumnType column, OgemaHttpRequest req) {
		if(show(req)) return Visibility.ENABLED;
		else return Visibility.HIDDEN;
	}
}
