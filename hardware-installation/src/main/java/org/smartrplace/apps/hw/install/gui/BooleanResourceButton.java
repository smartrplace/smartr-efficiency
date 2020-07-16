package org.smartrplace.apps.hw.install.gui;

import org.ogema.core.model.simple.BooleanResource;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

/** TODO: Move this to Util. Extracted from MultiSelectByButtons.
 * 
 * @author dnestle
 *
 */
public class BooleanResourceButton extends Button {
	protected final  BooleanResource res;
	protected final WidgetStyle<Button> selectedColor;
	protected final WidgetStyle<Button> deselectedColor;
	
	public BooleanResourceButton(WidgetPage<?> page, String id, String textDisplayed, BooleanResource res) {
		this(page, id, textDisplayed, res, ButtonData.BOOTSTRAP_GREEN,
				ButtonData.BOOTSTRAP_LIGHTGREY);
	}
	public BooleanResourceButton(WidgetPage<?> page, String id, String textDisplayed, BooleanResource res,
			WidgetStyle<Button> selectedColor,
			WidgetStyle<Button> deselectedColor) {
		super(page, id, textDisplayed);
		this.res = res;
		this.selectedColor = selectedColor;
		this.deselectedColor = deselectedColor;
		triggerOnPOST(this);
	}
	private static final long serialVersionUID = 1L;
	@Override
	public void onGET(OgemaHttpRequest req) {
		if(!res.getValue()) {
			//deselected
			removeStyle(selectedColor, req);
			addStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
			addStyle(deselectedColor, req);
		} else {
			//removeStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
			removeStyle(deselectedColor, req);
			addStyle(selectedColor, req);
		}
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		if(res.getValue()) {
			res.setValue(false);
		} else
			res.setValue(true);
	}
}
