package org.smartrplace.hwinstall.basetable;

import java.util.Arrays;
import java.util.List;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

/** An IntegerMultiButton switches between several integer states in a defined order
 * TODO: Move this to Util. Extracted from MultiSelectByButtons.
 * 
 * @author dnestle
 *
 */
public abstract class IntegerMultiButton extends Button {
	protected final List<WidgetStyle<Button>> optionColors;
	
	protected abstract String getText(int state, OgemaHttpRequest req);
	protected abstract int getState(OgemaHttpRequest req);
	protected abstract void setState(int state, OgemaHttpRequest req);
	
	@SuppressWarnings("unchecked")
	public IntegerMultiButton(WidgetPage<?> page, String id) {
		this(page, id, new WidgetStyle[] {ButtonData.BOOTSTRAP_LIGHTGREY, ButtonData.BOOTSTRAP_GREEN,
				ButtonData.BOOTSTRAP_RED});
	}
	public IntegerMultiButton(WidgetPage<?> page, String id,
			WidgetStyle<Button>[] optionColors) {
		this(page, id, Arrays.asList(optionColors));
	}
	public IntegerMultiButton(WidgetPage<?> page, String id,
			List<WidgetStyle<Button>> optionColors) {
		super(page, id);
		this.optionColors = optionColors;
		triggerOnPOST(this);
	}
	private static final long serialVersionUID = 1L;
	@Override
	public void onGET(OgemaHttpRequest req) {
		int state = getState(req);
		if(state < 0 || state >= optionColors.size())
			throw new IllegalStateException("State out of bounds:"+state+", optionColors size:"+optionColors.size());
		String text = getText(state, req);
		setText(text, req);
		for(int idx=0; idx<optionColors.size(); idx++) {
			if(idx == state)
				addStyle(optionColors.get(idx), req);
			else
				removeStyle(optionColors.get(idx), req);
		}
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		int prevstate = getState(req);
		int newState = prevstate+1;
		if(newState >= optionColors.size())
			newState = 0;
		setState(newState, req);
	}
}
