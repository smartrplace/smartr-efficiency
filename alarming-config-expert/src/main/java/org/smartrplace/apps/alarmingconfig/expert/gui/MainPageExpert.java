package org.smartrplace.apps.alarmingconfig.expert.gui;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPageExpert extends MainPage  {

	public MainPageExpert(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "3. Alarming Configuration Details Expert";
	}
}