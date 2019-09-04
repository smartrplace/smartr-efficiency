package org.smartrplace.smarteff.admin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.gui.ServiceDetailPage;
import org.smartrplace.smarteff.admin.gui.ServicePage;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;

import de.iwes.util.resource.ValueResourceHelper;
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
	public final ObjectGUITablePage<NavigationPageData, Resource> naviPage;
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
		if(!controller.appManExt.globalData().startPageId().isActive()) {
			//otherwise do not register start page yet
			pageServices = widgetApp.createStartPage();
		} else
			pageServices = widgetApp.createWidgetPage("pageService.html");
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
		if(controller.registerExtendedNaviMenuPages()) {
			menu.addEntry("Services Overview Page", pageServices);
			//menu.addEntry("Services Details Page", pageServiceDetails);
			menu.addEntry("Data Types", pageResTypes);
		}
		//menu.addEntry("Data Explorer", page3);
		if(!Boolean.getBoolean("org.smartrplace.smarteff.admin.excludeBaseStandardPages"))
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
	
	public boolean isStartPage(String pageId) {
		if(!controller.appManExt.globalData().startPageId().isActive()) return false;
		String startId = controller.appManExt.globalData().startPageId().getValue();
		if(startId.equals(pageId)) return true;
		return false;
	}
	public MenuConfiguration registerPage(NavigationPageData data, WidgetPage<?> page) {
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
		return mc;
	}
	
	public String getStartPageURL() {
		if(controller.appManExt.globalData().startPageId().isActive())
			return controller.appManExt.globalData().startPageId().getValue().replace(".", "_") + ".html";
		return null;
	}
	
	public void addPageToMenu(NavigationGUIProvider provider) {
		String id = SPPageUtil.buildId(provider);
		WidgetPage<?> page = allPages.get(id);
		if(page == null) throw new IllegalStateException("widget page must be created before addPageToMenu is called.");
		//Note: Existing entry for the NaviProvider is just overridden
		additionalEntries.put(id, new AdditionalMenuEntry(page, provider.label(null)));
		controller.appManExt.globalData().menuPageIds().create();
		ValueResourceUtils.appendValue(controller.appManExt.globalData().menuPageIds(), id);
		updatePageMenus();
	}
	
	public boolean removePageFromMenu(NavigationGUIProvider provider) {
		String id = SPPageUtil.buildId(provider);
		//WidgetPage<?> page = allPages.get(id);
		//if(page == null) throw new IllegalStateException("widget page must be created before addPageToMenu is called.");
		//Note: Existing entry for the NaviProvider is just overridden
		int index = getIndex(id, controller.appManExt.globalData().menuPageIds().getValues());
		if(index < 0) return false;
		AdditionalMenuEntry result = additionalEntries.remove(id);
		ValueResourceUtils.removeElement(controller.appManExt.globalData().menuPageIds(), index);
		updatePageMenus();
		return (result != null);
	}
	
	public boolean isInMenu(NavigationGUIProvider provider) {
		String id = SPPageUtil.buildId(provider);
		int index = getIndex(id, controller.appManExt.globalData().menuPageIds().getValues());
		if(index < 0) return false;
		else return true;
	}
	
	public boolean makeStartPage(NavigationGUIProvider provider) {
		if(!isInMenu(provider)) return false;
		String id = SPPageUtil.buildId(provider);
		ValueResourceHelper.setCreate(controller.appManExt.globalData().startPageId(), id);
		return true;
	}
	
	public boolean isStartPage(NavigationGUIProvider provider) {
		String id = SPPageUtil.buildId(provider);
		return isStartPage(id);
		//if(controller.appManExt.globalData().startPageId().isActive() &&
		//		controller.appManExt.globalData().startPageId().getValue().equals(id))
		//	return true;
		//return false;
	}

	public static int getIndex(String s, String[] arr) {
		int i = 0;
		for(String a: arr) {
			if(a.equals(s)) return i;
			i++;
		}
		return -1;
	}
}
