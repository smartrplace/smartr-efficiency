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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.apps.hw.install.HWInstallExtensionProvider;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert;
import org.smartrplace.apps.hw.install.gui.expert.RSSIPage;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service({Application.class, HWInstallExtensionProvider.class})
public class HardwareInstallAppExpert implements Application, HWInstallExtensionProvider {
	protected OgemaLogger log;
    protected ApplicationManager appMan;
    protected HardwareInstallController controller;

    protected WidgetApp widgetApp;
	public static final String urlPath = "/org/smartrplace/hardwareinstall/expert";

	@Reference
	private OgemaGuiService guiService;

	//@Reference
	//DatapointService dpService;
	
	GenericControllerReceiver<HardwareInstallController> controllerRecv =
			new GenericControllerReceiver<HardwareInstallController>() {

		@Override
		protected void controllerAndAppmanAvailable(HardwareInstallController controller,
				ApplicationManager appMan) {
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> page = widgetApp.createStartPage();
			controller.mainPageExts.add(new MainPageExpert(page, controller));
			
			WidgetPage<LocaleDictionary> rssiPageBase = widgetApp.createWidgetPage("rssipage.hmtl");
			new RSSIPage(rssiPageBase, controller);

			final NavigationMenu menu = new NavigationMenu(" Browse pages");
			menu.addEntry("Expert page", page);
			menu.addEntry("Communication quality page", rssiPageBase);
			page.getMenuConfiguration().setCustomNavigation(menu);
			rssiPageBase.getMenuConfiguration().setCustomNavigation(menu);
		}
	};
	
    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();
		controllerRecv.setAppman(appManager);
		//controller = new HardwareInstallControllerExpert(appMan, page, widgetApp, dpService);
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }

	@Override
	public void setController(HardwareInstallController controller) {
 		controllerRecv.setController(controller);
		/*if(this.controller != null)
 			return;
 		this.controller = controller;
		
 		//Init
		//register a web page with dynamically generated HTML
		while(appMan == null) try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
	}
}
