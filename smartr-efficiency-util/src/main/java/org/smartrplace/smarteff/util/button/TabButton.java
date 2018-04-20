package org.smartrplace.smarteff.util.button;

import java.util.HashSet;
import java.util.Set;

import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

public class TabButton extends Button {
	public final ButtonControlProvider control;
	
	public TabButton(WidgetPage<?> page, String id, String pid) {
		super(page, id+pid, SPPageUtil.OPEN_SAME_TAB_STRING);
		registerDependentWidget(this);
		
		control = new ButtonControlProviderImpl();

	}

	private static final long serialVersionUID = 1L;
	private boolean openNewTab(OgemaHttpRequest req) {
		return getText(req).equals(SPPageUtil.OPEN_NEW_TAB_STRING);
	}

	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		if(!openNewTab(req)) setText(SPPageUtil.OPEN_NEW_TAB_STRING, req);
		else setText(SPPageUtil.OPEN_SAME_TAB_STRING, req);
		for(RedirectButton b: buttonsToNotify) {
			b.setOpenInNewTab(control.openInNewTab(req), req);
		}
	}

	private Set<RedirectButton> buttonsToNotify = new HashSet<>();
	public class ButtonControlProviderImpl implements ButtonControlProvider {

		@Override
		public boolean openInNewTab(OgemaHttpRequest req) {
			return getText(req).equals(SPPageUtil.OPEN_NEW_TAB_STRING);
		}
		
		@Override
		public void registerRedirectButtonForStateSetting(RedirectButton button) {
			buttonsToNotify.add(button);
		}
	}
}
