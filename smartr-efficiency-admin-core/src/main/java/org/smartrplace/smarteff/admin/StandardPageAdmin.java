package org.smartrplace.smarteff.admin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.gui.NaviOverviewPage;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.gui.ServiceDetailPage;
import org.smartrplace.smarteff.admin.gui.ServicePage;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

public class StandardPageAdmin {
	public static final String NAVI_OVERVIEW_URL = "naviOverview.html";
	public final ServicePage mainPage;
	public final ServiceDetailPage offlineEvalPage;
	public final ResTypePage resTypePage;
	//public final DataExplorerPage dataExPage;
	public final NaviOverviewPage naviPage;
	public NavigationMenu menu;

	private final WidgetPage<?> pageServices;
	private final WidgetPage<?> pageServiceDetails;
	private final WidgetPage<?> pageResTypes;
	private final WidgetPage<?> pageNavis;
	//private final WidgetPage<?> page3;
	
	private final SpEffAdminController controller;
	
	public class AdditionalMenuEntry {
		public AdditionalMenuEntry(WidgetPage<?> page, String label) {
			this.page = page;
			this.label = label;
		}
		WidgetPage<?> page;
		String label;
	}
	Map<String, AdditionalMenuEntry> additionalEntries = new LinkedHashMap<>();
	//pageId -> page
	Map<String, WidgetPage<?>> allPages = new HashMap<>();
	
	public StandardPageAdmin(WidgetApp widgetApp, SpEffAdminController controller) {
		this.controller = controller;
		
		//register a web page with dynamically generated HTML
		pageServices = widgetApp.createStartPage();
		mainPage = new ServicePage(pageServices, controller);
		pageServiceDetails = widgetApp.createWidgetPage("Details.html");
		offlineEvalPage = new ServiceDetailPage(pageServiceDetails, controller);
		pageResTypes = widgetApp.createWidgetPage("resTypes.html");
		SmartrEffExtResourceTypeData rtd = new SmartrEffExtResourceTypeData(BaseDataService.BUILDING_DATA, null, null);
		resTypePage = controller.getResTypePage(pageResTypes, rtd);
		//page3 = widgetApp.createWidgetPage("dataExplorer.html");
		//dataExPage = new DataExplorerPage(page3, controller, controller.getUserAdmin().getAppConfigData().globalData());
		pageNavis = widgetApp.createWidgetPage(NAVI_OVERVIEW_URL);
		NavigationPageData navi = new NavigationPageData(BaseDataService.RESOURCEALL_NAVI_PROVIDER, null, "", null);
		naviPage = controller.getNaviPage(pageNavis, navi);

		updatePageMenus();
	}
	
	private void updatePageMenus() {
		menu = new NavigationMenu("Select Page");
		menu.addEntry("Services Overview Page", pageServices);
		//menu.addEntry("Services Details Page", pageServiceDetails);
		menu.addEntry("Data Types", pageResTypes);
		//menu.addEntry("Data Explorer", page3);
		menu.addEntry("Navigation Pages", pageNavis);
		
		for(AdditionalMenuEntry add: additionalEntries.values()) {
			menu.addEntry(add.label, add.page);
		}
		
		MenuConfiguration mc = pageServices.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = pageServiceDetails.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = pageResTypes.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		//mc = page3.getMenuConfiguration();
		//mc.setCustomNavigation(menu);
		mc = pageNavis.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		
		for(WidgetPage<?> all: allPages.values()) {
			mc = all.getMenuConfiguration();
			mc.setCustomNavigation(menu);			
		}
	}
	
	public void registerPage(NavigationPageData data, WidgetPage<?> page) {
		String id = SPPageUtil.buildId(data.provider);
		allPages.put(id, page);
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		
		for(String add: controller.appManExt.globalData().menuPageIds().getValues()) {
			if(id.equals(add)) {
				addPageToMenu(data.provider);
				break;
			}
		}
	}
	public void addPageToMenu(NavigationGUIProvider provider) {
		String id = SPPageUtil.buildId(provider);
		WidgetPage<?> page = allPages.get(id);
		if(page == null) throw new IllegalStateException("widget page must be created before addPageToMenu is called.");
		//Note: Existing entry for the NaviProvider is just overridden
		additionalEntries.put(id, new AdditionalMenuEntry(page, provider.label(null)));
		ValueResourceUtils.appendValue(controller.appManExt.globalData().menuPageIds(), id);
		updatePageMenus();
	}
	//TODO: Implement removePageFromMenu
	
}
