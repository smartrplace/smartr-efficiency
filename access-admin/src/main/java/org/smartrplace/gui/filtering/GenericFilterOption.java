package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public interface GenericFilterOption<T> extends GenericFilterI<T> {
	Map<OgemaLocale, String> optionLabel();
}
