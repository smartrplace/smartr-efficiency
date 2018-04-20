package org.smartrplace.extensionservice;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

public interface ExtensionCapability extends LabelledItem, ExtensionCapabilityPublicData {
	/**Id of service. If null the full class name shall be used as id*/
	default String id() {
		return null;
	}
	
	@Override
	default String description(OgemaLocale locale) {
		return label(locale);
	}

	default String userName() {return null;}
}
