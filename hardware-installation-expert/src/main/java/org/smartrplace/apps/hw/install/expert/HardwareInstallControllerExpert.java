/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.hw.install.expert;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert;
import org.smartrplace.apps.hw.install.gui.expert.RSSIPage;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

// here the controller logic is implemented
public class HardwareInstallControllerExpert extends HardwareInstallController {
	protected final WidgetApp widgetApp;
	
	public HardwareInstallControllerExpert(ApplicationManager appMan, WidgetPage<?> page, WidgetApp widgetApp,
			DatapointService dpService) {
		super(appMan, page, null, dpService);
		this.widgetApp = widgetApp;
		
		WidgetPage<LocaleDictionary> rssiPageBase = widgetApp.createWidgetPage("rssipage.hmtl");
		RSSIPage rssiPage = new RSSIPage(rssiPageBase, this);

		final NavigationMenu menu = new NavigationMenu(" Browse pages");
		menu.addEntry("Expert page", page);
		menu.addEntry("Communication quality page", rssiPageBase);
		page.getMenuConfiguration().setCustomNavigation(menu);
		rssiPageBase.getMenuConfiguration().setCustomNavigation(menu);
	}

	@Override
	protected MainPage getMainPage(WidgetPage<?> page) {
		return new MainPageExpert(page, this);
	}

}
