package org.smartrplace.smarteff.util.button;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public interface CreateButtonI {
	Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData, OgemaHttpRequest req);
}
