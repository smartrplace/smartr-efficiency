package org.smartrplace.extensionservice;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.model.prototypes.Data;

public interface ExtensionUserData extends Data {
	StringArrayResource menuPageIds();
}
