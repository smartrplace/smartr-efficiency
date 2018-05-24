package org.smartrplace.extensionservice;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

public interface ExtensionUserData extends Data {
	StringArrayResource menuPageIds();
	StringResource startPageId();
}
