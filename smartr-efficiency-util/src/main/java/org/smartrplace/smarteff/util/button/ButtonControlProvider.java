package org.smartrplace.smarteff.util.button;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;

public interface ButtonControlProvider {
	default boolean openInNewTab(OgemaHttpRequest req) {return false;}
	
	default void registerRedirectButtonForStateSetting(RedirectButton button) {}
}
