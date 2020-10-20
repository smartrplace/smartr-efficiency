package org.smartrplace.apps.hw.install.gui.expert;

import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.gui.MainPage;

import de.iwes.widgets.api.widgets.WidgetPage;

@Deprecated
public class RSSIPage extends MainPage {
	
	@Override
	protected String getHeader() {return "Smartrplace Communication Quality Supervision Page";}

	public RSSIPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, true);
	}
}
