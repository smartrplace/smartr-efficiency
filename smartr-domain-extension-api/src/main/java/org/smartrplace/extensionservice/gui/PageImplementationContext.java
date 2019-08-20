package org.smartrplace.extensionservice.gui;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface PageImplementationContext {
	/** Only relevant for GenericEditPages: Get labels of fields*/
	Map<String, Map<OgemaLocale, String>> getLabels();
}
